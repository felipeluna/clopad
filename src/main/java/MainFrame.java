import clojure.lang.Compiler;
import clojure.lang.*;
import freditor.FreditorUI;
import freditor.LineNumbers;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

public class MainFrame extends JFrame {
    private Editor input;
    private JButton button;
    private FreditorUI output;
    private FreditorUI source;
    private JTabbedPane tabs;

    public MainFrame() {
        super(Editor.filename);

        input = new Editor();
        input.onRightClick = this::printSource;
        button = new JButton("evaluate");
        button.addActionListener(this::evaluate);
        output = new FreditorUI(OutputFlexer.instance, ClojureIndenter.instance, 80, 10);
        source = new FreditorUI(Flexer.instance, ClojureIndenter.instance, 80, 10);
        source.onRightClick = this::printSource;

        JPanel inputWithLineNumbers = new JPanel();
        inputWithLineNumbers.setLayout(new BoxLayout(inputWithLineNumbers, BoxLayout.X_AXIS));
        inputWithLineNumbers.add(new LineNumbers(input));
        inputWithLineNumbers.add(input);
        input.setComponentToRepaint(inputWithLineNumbers);

        JPanel buttons = new JPanel();
        buttons.add(button);
        tabs = new JTabbedPane();
        tabs.addTab("output", output);
        tabs.addTab("source", source);
        JPanel down = new JPanel(new BorderLayout());
        down.add(buttons, BorderLayout.NORTH);
        down.add(tabs, BorderLayout.CENTER);

        add(new JSplitPane(JSplitPane.VERTICAL_SPLIT, inputWithLineNumbers, down));

        boringStuff();
    }

    private void evaluate(ActionEvent event) {
        input.tryToSaveCode();
        StringWriter console = new StringWriter();
        Var.pushThreadBindings(RT.map(RT.OUT, console));
        try {
            input.requestFocusInWindow();
            String text = input.getText();
            Reader reader = new StringReader(text);
            Object result = Compiler.load(reader, Editor.directory, "clopad.txt");
            if (result instanceof IPending) {
                final int PENDING_LIMIT = 100;
                result = Clojure.take.invoke(PENDING_LIMIT, result);
            }
            RT.print(result, console);
        } catch (Compiler.CompilerException ex) {
            console.append(ex.getCause().getMessage());
            if (ex.line > 0) {
                String message = ex.getMessage();
                int colon = message.lastIndexOf(':');
                int paren = message.lastIndexOf(')');
                int column = Integer.parseInt(message.substring(colon + 1, paren));
                input.setCursorTo(ex.line - 1, column - 1);
            }
        } catch (IOException impossible) {
            throw new RuntimeException(impossible);
        } finally {
            Var.popThreadBindings();
            output.loadFromString(console.toString());
            tabs.setSelectedComponent(output);
        }
    }

    public void printSource(String lexeme) {
        StringWriter console = new StringWriter();
        Var.pushThreadBindings(RT.map(RT.OUT, console, RT.CURRENT_NS, RT.CURRENT_NS.deref()));
        try {
            String text = input.getText();
            Reader reader = new StringReader(text);
            LineNumberingPushbackReader rdr = new LineNumberingPushbackReader(reader);
            Object nsForm = LispReader.read(rdr, false, null, false, null);
            Compiler.eval(nsForm, false);

            Object symbol = Clojure.symbol.invoke(lexeme);
            Object source = Clojure.sourceFn.invoke(symbol);
            if (source != null) {
                console.append(source.toString());
            }
        } catch (LispReader.ReaderException ex) {
            console.append(ex.getCause().getMessage());
        } finally {
            Var.popThreadBindings();
            source.loadFromString(console.toString());
            tabs.setSelectedComponent(source);
        }
    }

    private void boringStuff() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                input.tryToSaveCode();
            }
        });
        pack();
        setVisible(true);
        input.requestFocusInWindow();
    }
}
