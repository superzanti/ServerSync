package com.superzanti.serversync.util.minecraft.config;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Used to create and update the client side config file for Serversync
 *
 * @author Rheimus
 */
public class FriendlyConfigWriter extends BufferedWriter {

    public int indentation = 0;

    public FriendlyConfigWriter(BufferedWriter writer) throws IOException {
        super(writer);
        this.write("# Configuration file");
        this.newLines(2);
        this.flush();
    }

    /**
     * Writes an opening header for <strong>name</strong> category
     * subsequent calls to writeElement() will be added to this category until
     * writeCloseCategory() has been called. Nesting indentation is handled automatically
     *
     * @param name
     * @throws IOException
     */
    public void writeOpenCategory(String name) throws IOException {
        this.writeWithIndentation(name + " {");
        this.newLine();
        indentation++;
    }

    public void writeCloseCategory() throws IOException {
        indentation--;
        this.writeWithIndentation("}");
        this.newLines(2);
    }

    /**
     * Writes this element out in its entirety, this will not automatically add new
     * lines after the element however it will automatically handle indentation
     *
     * @param element Element to write
     * @throws IOException
     */
    public void writeElement(FriendlyConfigElement element) throws IOException {
        if (element.hasComment) {
            ArrayList<String> comments = element.getComments();
            for (String comment : comments) {
                this.writeWithIndentation("# " + comment);
                this.newLine();
            }
        }

        if (element.isArray) {
            ArrayList<String> values = element.getList();

            // Header for array element
            this.writeWithIndentation(element.getTypeTag() + ":" + element.getName() + " <");
            indentation++;
            for (String value : values) {
                this.newLine();
                this.writeWithIndentation(value);
            }
            indentation--;
            // Footer for array element
            this.newLine();
            this.writeWithIndentation(">");
        } else {
            this.writeWithIndentation(element.getTypeTag() + ":" + element.getName() + "=" + element.getString());
        }
    }

    public String indent(int num) {
        StringBuilder indent = new StringBuilder(4);
        for (int i = 0; i < num; i++) {
            indent.append("    ");
        }
        return indent.toString();
    }

    private void writeWithIndentation(String str) throws IOException {
        indent();
        this.write(str);
    }

    private void indent() throws IOException {
        if (indentation > 0) {
            this.write(indent(indentation));
        }
    }

    /**
     * Writes multiple sets of new lines, equivalent to writing newLine() <strong>num</strong>
     * number of times
     *
     * @param num number of new lines to write
     * @throws IOException
     */
    public void newLines(int num) throws IOException {
        for (int i = 0; i < num; i++) {
            this.newLine();
        }
    }

}
