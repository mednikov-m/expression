package com.github.expression.token;

import com.github.expression.exception.ParseException;

import java.util.*;

public class TokenExtractor {

    private List<Character> exitSymbols = Arrays.asList(' ', '\t', '\n', '\r', '\f');
    private List<Character> delimiterSymbols = Arrays.asList('"', '\'', ',', '(', ')', '[', ']', '&', '/', '+', '-');

    public List<String> getTokens(String line) throws ParseException {
        List<String> tokens = new ArrayList<>();
        int	n = 0;

        do {
            // Extract the next word token from the line
            TokenData data = getToken(line, n);
            n = data.getPosition();
            // Add the word token to the list
            if (n > 0) {
                tokens.add(data.getExtractedToken());
            }

        } while (n > 0);

        return tokens.isEmpty() ? Collections.emptyList() : tokens;
    }

    /***************************************************************************
     * Extract the next word token from an SQL-like query expression.
     *
     * @param	line
     * A string containing one or more SQL-like query expression word tokens
     * separated by whitespace.
     *
     * @param	startPosition
     * Position within <tt>line</tt> where token parsing is to begin.
     *
     * @return
     * The position of the character following the last one parsed from
     * <tt>line</tt>, or zero if there are no more word tokens to extract from
     * the line.  The returned value can serve as the starting parse position in
     * a subsequent call to this method.
     *
     * @throws	ParseException
     * Thrown if a token is malformed, such as missing a closing quote.
     *
     */
    private TokenData getToken(String line, int startPosition) throws ParseException {
        StringBuilder tokenData = new StringBuilder();
        LineMetadata skipLeadingSpacesMetadata = skipLeadingSpaces(line, startPosition);
        int currentPosition = skipLeadingSpacesMetadata.getCurrentPosition();
        if (currentPosition >= line.length()) {
            return new TokenData("", 0);
        }
        LineMetadata leadingQuoteMetadata = checkLeadingQuote(skipLeadingSpacesMetadata, tokenData);
        char quote = leadingQuoteMetadata.getCurrentChar();
        currentPosition = leadingQuoteMetadata.getCurrentPosition();
        currentPosition = parseInternal(currentPosition, line, quote, tokenData);
        return new TokenData(tokenData.toString(), currentPosition);
    }

    private LineMetadata checkLeadingQuote(LineMetadata metadata, StringBuilder token) {
        char quote = metadata.getCurrentChar();
        int tmpCounter = metadata.getCurrentPosition();
        if (quote == '"' || quote == '\'') {
            token.append(metadata.getCurrentChar());
            tmpCounter++;
        } else if (quote == '`') {
            tmpCounter++;
        } else {
            quote = ' ';
        }
        return new LineMetadata(tmpCounter, quote);
    }

    private int parseInternal(int currentPosition, String line, char quote, StringBuilder tok) throws ParseException {
        char ch = ' ';
        int i = currentPosition;
        while (i < line.length()) {
            ch = line.charAt(i++);
            if (quote == ' ') {
                if (exitSymbols.contains(ch)) {
                    return calculateFinalPosition(ch, quote, --i);
                } else if (delimiterSymbols.contains(ch)) {
                    return handleDelimiter(tok, ch, quote, i);
                } else if (Arrays.asList('*', '|').contains(ch)) {
                    return handleAsteriskOrPipe(line, tok, ch, quote, i);
                } else if (ch == '\\') {
                    LineMetadata escapedMetadata = handleEscapedChar(line, tok, i);
                    i = escapedMetadata.getCurrentPosition();
                    ch = escapedMetadata.getCurrentChar();
                } else if (ch == '.') {
                    WrapperResult result = handleDot(tok, line, i, ch);
                    if (result.status) {
                        return result.getMetadata().getCurrentPosition();
                    } else {
                        LineMetadata metadata = result.getMetadata();
                        i = metadata.getCurrentPosition();
                        ch = metadata.getCurrentChar();
                    }
                } else {
                    tok.append(ch);
                }
            } else {
                WrapperResult result = handleQuote(tok, line, ch, quote, i);
                LineMetadata quoteMetadata = result.getMetadata();
                i = quoteMetadata.getCurrentPosition();
                ch = quoteMetadata.getCurrentChar();
                if (result.status) {
                    return i;
                }
            }
        }
        return calculateFinalPosition(ch, quote, i);
    }

    private int calculateFinalPosition(char currentChar, char quote, int currentPosition) throws ParseException {
        checkMissingClosingQuote(currentChar, quote, currentPosition);
        return currentPosition;
    }

    private LineMetadata appendQuote(String line,
                                     int currentPosition,
                                     char quote,
                                     StringBuilder data) throws ParseException {
        char currentSymbol;
        if (currentPosition < line.length()) {
            currentSymbol = line.charAt(currentPosition++);
            if (currentSymbol == quote) {
                data.append(currentSymbol);
            } else {
                data.append('\\');
                currentPosition--;
            }
        } else {
            throw new ParseException("Missing closing quote (" + quote + ")", currentPosition);
        }
        return new LineMetadata(currentPosition, currentSymbol);
    }

    private void checkMissingClosingQuote(char currentChar, char quote, int currentPosition) throws ParseException {
        if (quote != ' ' && currentChar != quote) {
            throw new ParseException("Missing closing quote (" + quote + ")", currentPosition);
        }
    }

    private LineMetadata skipLeadingSpaces(String line, int currentPosition) {
        char ch = ' ';
        while (currentPosition < line.length())
        {
            ch = line.charAt(currentPosition);
            if (!exitSymbols.contains(ch)) {
                break;
            }
            currentPosition++;
        }
        return new LineMetadata(currentPosition, ch);
    }


    private int handleDelimiter(StringBuilder token, char currentSymbol, char quote, int currentPosition) throws ParseException {
        if (token.length() == 0) {
            token.append(currentSymbol);
        } else {
            currentPosition--;
        }
        return calculateFinalPosition(currentSymbol, quote, currentPosition);
    }

    private int handleAsteriskOrPipe(String line, StringBuilder token, char currentChar, char quote, int currentPosition) throws ParseException {
        if (token.length() == 0) {
            token.append(currentChar);
            // Handle '**' and '||' operators
            if (currentPosition < line.length()) {
                char tmp = line.charAt(currentPosition++);
                if (tmp == currentChar) {
                    token.append(tmp);
                } else {
                    currentPosition--;
                }
            }
        } else {
            currentPosition--;
        }
        return calculateFinalPosition(currentChar, quote, currentPosition);
    }

    private WrapperResult handleQuote(StringBuilder token, String line, char currentSymbol, char quote, int currentPosition) throws ParseException {
        if (currentSymbol == quote) {
            if (quote != '`') {
                token.append(currentSymbol);
            }
            return new WrapperResult(true, new LineMetadata(currentPosition, currentSymbol));
        }
        else if (currentSymbol == '\\')
        {
            LineMetadata appendedQuoteMetaData = appendQuote(line, currentPosition, quote, token);
            currentPosition = appendedQuoteMetaData.getCurrentPosition();
            currentSymbol = appendedQuoteMetaData.getCurrentChar();
        }
        else {
            token.append(currentSymbol);
        }
        return new WrapperResult(false, new LineMetadata(currentPosition, currentSymbol));
    }

    private LineMetadata handleEscapedChar(String line, StringBuilder token, int currentPosition) throws ParseException {
        char currentChar;
        if (currentPosition < line.length()) {
            currentChar = line.charAt(currentPosition++);
            token.append(currentChar);
        }
        else {
            throw new ParseException("Missing escaped character", currentPosition);
        }
        return new LineMetadata(currentPosition, currentChar);
    }

    private WrapperResult handleDot(StringBuilder token, String line, int currentPosition, char currentChar) throws ParseException {
        int		j, k;
        k = token.length();
        if (k == 0)
        {
            token.append(currentChar);
            if (currentPosition < line.length()  &&  Character.isDigit(line.charAt(currentPosition)))
                return new WrapperResult(false , new LineMetadata(currentPosition, currentChar));
            else
                return new WrapperResult(true, new LineMetadata(calculateFinalPosition(currentChar, ' ', currentPosition), currentChar));
        }

        j = 0;
        currentChar = token.charAt(0);
        if (currentChar == '+'  ||  currentChar == '-')
            j++;
        while (j < k) {
            currentChar = token.charAt(j++);
            if (!Character.isDigit(currentChar)) {
                // Delimiter punctuation
                currentPosition--;
                return new WrapperResult(true, new LineMetadata(calculateFinalPosition(currentChar, ' ', currentPosition), currentChar));
            }
        }

        // Numeric decimal pt
        token.append('.');
        return new WrapperResult(false, new LineMetadata(currentPosition, currentChar));
    }

    private class WrapperResult {
        private boolean status;
        private LineMetadata metadata;

        public WrapperResult(boolean status, LineMetadata metadata) {
            this.status = status;
            this.metadata = metadata;
        }

        public boolean isStatus() {
            return status;
        }

        public LineMetadata getMetadata() {
            return metadata;
        }
    }
}
