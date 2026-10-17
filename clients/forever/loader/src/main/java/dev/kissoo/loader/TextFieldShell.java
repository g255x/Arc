package dev.kissoo.loader;

import javax.swing.JComponent;
import java.awt.BorderLayout;

final class TextFieldShell extends RoundPanel {
    TextFieldShell(JComponent child) {
        super(18, Palette.light() ? new java.awt.Color(255, 255, 255, 166) : new java.awt.Color(8, 9, 8, 150));
        setLayout(new BorderLayout());
        add(child);
    }
}
