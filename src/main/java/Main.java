import clojure.lang.RT;
import freditor.SwingConfig;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingConfig.metalWithDefaultFont(SwingConfig.SANS_SERIF_PLAIN_16);
        SwingUtilities.invokeLater(() -> new MainFrame(args.length >= 1 ? args[0] : Editor.defaultFilename));
        RT.init();
    }
}
