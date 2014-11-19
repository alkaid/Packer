package com.alkaid.packer.util;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Properties;

public class SafeProperties extends Properties {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5397205771734185048L;
	@Override
	/**
     * Stores the mappings in this {@code Properties} object to {@code out},
     * putting the specified comment at the beginning.
     *
     * @param writer the {@code Writer}
     * @param comment an optional comment to be written, or null
     * @throws IOException
     * @throws ClassCastException if a key or value is not a string
     * @since 1.6
     */
    public synchronized void store(Writer writer, String comment) throws IOException {
        if (comment != null) {
            writer.write("#");
            writer.write(comment);
            writer.write(System.lineSeparator());
        }
        /*writer.write("#");
        writer.write(new Date().toString());
        writer.write(System.lineSeparator());*/

        StringBuilder sb = new StringBuilder(200);
        for (Map.Entry<Object, Object> entry : entrySet()) {
            String key = (String) entry.getKey();
            dumpString(sb, key, true);
            sb.append('=');
            dumpString(sb, (String) entry.getValue(), false);
            sb.append(System.lineSeparator());
            writer.write(sb.toString());
            sb.setLength(0);
        }
        writer.flush();
    }
	
	private void dumpString(StringBuilder buffer, String string, boolean key) {
        int i = 0;
        if (!key && i < string.length() && string.charAt(i) == ' ') {
            buffer.append("\\ ");
            i++;
        }

        for (; i < string.length(); i++) {
            char ch = string.charAt(i);
            switch (ch) {
            case '\t':
                buffer.append("\\t");
                break;
            case '\n':
                buffer.append("\\n");
                break;
            case '\f':
                buffer.append("\\f");
                break;
            case '\r':
                buffer.append("\\r");
                break;
            default:
                if ("\\#!=:".indexOf(ch) >= 0 || (key && ch == ' ')) {
                    buffer.append('\\');
                }
                if (ch >= ' ' && ch <= '~') {
                    buffer.append(ch);
                } else {
                    String hex = Integer.toHexString(ch);
                    buffer.append("\\u");
                    for (int j = 0; j < 4 - hex.length(); j++) {
                        buffer.append("0");
                    }
                    buffer.append(hex);
                }
            }
        }
    }
}
