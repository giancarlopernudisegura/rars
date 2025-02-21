package rars.venus;

import rars.Globals;
import rars.Settings;
import rars.simulator.Simulator;
import rars.venus.registers.*;
import rars.venus.run.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyVetoException;
import java.net.URL;

/*
Copyright (c) 2003-2006, Siva Chowdeswar Nandipati & Giancarlo Pernudi Segura

Developed by Siva Chowdeswar Nandipati (sivachow@ualberta.ca) and Giancarlo (pernudi@ualberta.ca)

Permission is hereby granted, free of charge, to any person obtaining 
a copy of this software and associated documentation files (the 
"Software"), to deal in the Software without restriction, including 
without limitation the rights to use, copy, modify, merge, publish, 
distribute, sublicense, and/or sell copies of the Software, and to 
permit persons to whom the Software is furnished to do so, subject 
to the following conditions:

The above copyright notice and this permission notice shall be 
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

(MIT license, http://www.opensource.org/licenses/mit-license.html)
 */

/**
 * Top level container for General Venus GUI.
 *
 * @author Siva, Giancarlo, Sanderson, and Team JSpim
 **/
public class GeneralVenusUI extends JFrame {
    GeneralVenusUI mainUI;

    private JToolBar toolbar;
    private GeneralMainPane mainPane;
    private GeneralRegistersPane registersPane;
    private RegistersWindow registersTab;
    private FloatingPointWindow fpTab;
    private ControlAndStatusWindow csrTab;
    private JSplitPane horizonSplitter;
    JPanel north;

    private JButton Run, Reset, Step, Backstep, Stop, Pause;
    private Action runGoAction, runStepAction, runBackstepAction,
            runResetAction, runStopAction, runPauseAction;
    private final int hart;

    // PLEASE PUT THESE TWO (& THEIR METHODS) SOMEWHERE THEY BELONG, NOT HERE
    private boolean reset = true; // registers/memory reset for execution
    private boolean started = false;  // started execution

    /**
     * Constructor for the Class. Sets up a window object for the UI
     *
     * @param s Name of the window to be created.
     **/

    // TODO check for mem observer
    public GeneralVenusUI(int hart) {
        super(String.format("Hart %d", hart + 1));
        mainUI = this;
        this.hart = hart;
        this.createActionObjects();
        VenusUI.observers.add(this);
        double screenWidth = Toolkit.getDefaultToolkit().getScreenSize().getWidth();
        double screenHeight = Toolkit.getDefaultToolkit().getScreenSize().getHeight();
        double mainWidthPct = (screenWidth < 1000.0) ? 0.67 : 0.73;
        double mainHeightPct = (screenWidth < 1000.0) ? 0.60 : 0.65;
        double registersWidthPct = (screenWidth < 1000.0) ? 0.18 : 0.22;
        double registersHeightPct = (screenWidth < 1000.0) ? 0.72 : 0.80;
        Dimension mainPanePreferredSize = new Dimension((int) (screenWidth * mainWidthPct), (int) (screenHeight * mainHeightPct));
        Dimension registersPanePreferredSize = new Dimension((int) (screenWidth * registersWidthPct), (int) (screenHeight * registersHeightPct));
        Globals.initialize(true);

        //  image courtesy of NASA/JPL.
        URL im = this.getClass().getResource(Globals.imagesPath + "RISC-V.png");
        if (im == null) {
            System.out.println("Internal Error: images folder or file not found");
            System.exit(0);
        }
        Image mars = Toolkit.getDefaultToolkit().getImage(im);
        this.setIconImage(mars);
        // Everything in frame will be arranged on JPanel "center", which is only frame component.
        // "center" has BorderLayout and 2 major components:
        //   -- panel (jp) on North with 2 components
        //      1. toolbar
        //      2. run speed slider.
        //   -- split pane (horizonSplitter) in center with 2 components side-by-side
        //      1. split pane (splitter) with 2 components stacked
        //         a. main pane, with 2 tabs (edit, execute)
        //         b. messages pane with 2 tabs (rars, run I/O)
        //      2. registers pane with 3 tabs (register file, coproc 0, coproc 1)
        // I should probably run this breakdown out to full detail.  The components are created
        // roughly in bottom-up order; some are created in component constructors and thus are
        // not visible here.

        registersTab = new RegistersWindow(hart);
        fpTab = new FloatingPointWindow(hart);
        csrTab = new ControlAndStatusWindow(hart);
        registersPane = new GeneralRegistersPane(mainUI, registersTab, fpTab, csrTab);
        registersPane.setPreferredSize(registersPanePreferredSize);

        mainPane = new GeneralMainPane(mainUI, registersTab, fpTab, csrTab, hart);
        mainPane.setPreferredSize(mainPanePreferredSize);
        try {
            mainPane.getExecutePane().getTextSegmentWindow().setMaximum(true);
        } catch (PropertyVetoException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        horizonSplitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mainPane, registersPane);
        horizonSplitter.setOneTouchExpandable(true);
        horizonSplitter.resetToPreferredSizes();

        toolbar = this.setUpToolBar();

        JPanel jp = new JPanel(new FlowLayout(FlowLayout.LEFT));
        jp.add(toolbar);
        JPanel center = new JPanel(new BorderLayout());
        center.add(jp, BorderLayout.NORTH);
        center.add(horizonSplitter);
        this.add(center);

        this.addWindowListener(
            new WindowAdapter() {
                    // This is invoked when opening the app.  It will set the app to
                    // appear at full screen size.
                    public void windowOpened(WindowEvent e) {
                        mainUI.pack();
                        mainUI.setMenuState();
                    }

                    // This is invoked when closing the app.
                    public void windowClosed(WindowEvent e) {
                        VenusUI.observers.remove(mainUI);
                    }
                });
    }

    /**
     * To set whether the register values are reset.
     *
     * @param b Boolean true if the register values have been reset.
     **/

    public void setReset(boolean b) {
        reset = b;
    }

    /**
     * To set whether MIPS program execution has started.
     *
     * @param b true if the MIPS program execution has started.
     **/

    public void setStarted(boolean b) {
        started = b;
    }

    /**
     * To find out whether the register values are reset.
     *
     * @return Boolean true if the register values have been reset.
     **/

    public boolean getReset() {
        return reset;
    }

    /**
     * To find out whether MIPS program is currently executing.
     *
     * @return true if MIPS program is currently executing.
     **/
    public boolean getStarted() {
        return started;
    }

    /**
     * Get reference to messages pane associated with this GUI.
     *
     * @return MessagesPane object associated with the GUI.
     **/

    public GeneralMainPane getMainPane() {
        return mainPane;
    }

    /**
     * Get reference to registers pane associated with this GUI.
     *
     * @return RegistersPane object associated with the GUI.
     **/

    public GeneralRegistersPane getRegistersPane() {
        return registersPane;
    }


    private ImageIcon loadIcon(String name) {
        return new ImageIcon(Toolkit.getDefaultToolkit().getImage(this.getClass().getResource(Globals.imagesPath + name)));
    }

    private KeyStroke makeShortcut(int key) {
        return KeyStroke.getKeyStroke(key, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
    }

    /*
     * Action objects are used instead of action listeners because one can be easily
     * shared between a menu item and a toolbar button. Does nice things like
     * disable both if the action is disabled, etc.
     */
    private void createActionObjects() {
        try {
            runGoAction = new RunGoAction("Go", loadIcon("Play22.png"), "Run the current program", KeyEvent.VK_G,
                    KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), Globals.getGui());
            runStepAction = new RunStepAction("Step", loadIcon("StepForward22.png"), "Run one step at a time",
                    KeyEvent.VK_T, KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0), Globals.getGui());
            runBackstepAction = new RunBackstepAction("Backstep", loadIcon("StepBack22.png"), "Undo the last step",
                    KeyEvent.VK_B, KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0), Globals.getGui());
            runPauseAction = new GuiAction("Pause", loadIcon("Pause22.png"), "Pause the currently running program",
                    KeyEvent.VK_P, KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)) {
                public void actionPerformed(ActionEvent e) {
                    Simulator.getInstance().pauseExecution();
                    // RunGoAction's "paused" method will do the cleanup.
                }
            };
            runStopAction = new GuiAction("Stop", loadIcon("Stop22.png"), "Stop the currently running program",
                    KeyEvent.VK_S, KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)) {
                public void actionPerformed(ActionEvent e) {
                    Simulator.getInstance().stopExecution();
                    // RunGoAction's "stopped" method will take care of the cleanup.
                }
            };
            runResetAction = new RunResetAction("Reset", loadIcon("Reset22.png"), "Reset memory and registers",
                    KeyEvent.VK_R, KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0), Globals.getGui());
        } catch (NullPointerException e) {
            System.out.println(
                    "Internal Error: images folder not found, or other null pointer exception while creating Action objects");
            e.printStackTrace();
            System.exit(0);
        }
    }

    /*
     * build the toolbar and connect items to action objects (which serve as action
     * listeners shared between toolbar icon and corresponding menu item).
     */

    JToolBar setUpToolBar() {
        JToolBar toolBar = new JToolBar();

        Run = new JButton(runGoAction);
        Run.setText("");
        Step = new JButton(runStepAction);
        Step.setText("");
        Backstep = new JButton(runBackstepAction);
        Backstep.setText("");
        Reset = new JButton(runResetAction);
        Reset.setText("");
        Stop = new JButton(runStopAction);
        Stop.setText("");
        Pause = new JButton(runPauseAction);
        Pause.setText("");

        toolBar.add(Run);
        toolBar.add(Step);
        toolBar.add(Backstep);
        toolBar.add(Pause);
        toolBar.add(Stop);
        toolBar.add(Reset);

        return toolBar;
    }

    /*
     * Determine from FileStatus what the menu state (enabled/disabled)should be
     * then call the appropriate method to set it. Current states are:
     *
     * setMenuStateInitial: set upon startup and after File->Close
     * setMenuStateEditingNew: set upon File->New setMenuStateEditing: set upon
     * File->Open or File->Save or erroneous Run->Assemble setMenuStateRunnable: set
     * upon successful Run->Assemble setMenuStateRunning: set upon Run->Go
     * setMenuStateTerminated: set upon completion of simulated execution
     */
    protected void setMenuState() {
        int status = VenusUI.getMenuState();
        switch (status) {
            case FileStatus.NO_FILE:
                setMenuStateInitial();
                break;
            case FileStatus.NEW_NOT_EDITED:
                setMenuStateEditingNew();
                break;
            case FileStatus.NEW_EDITED:
                setMenuStateEditingNew();
                break;
            case FileStatus.NOT_EDITED:
                setMenuStateNotEdited(); // was MenuStateEditing. DPS 9-Aug-2011
                break;
            case FileStatus.EDITED:
                setMenuStateEditing();
                break;
            case FileStatus.RUNNABLE:
                setMenuStateRunnable();
                break;
            case FileStatus.RUNNING:
                setMenuStateRunning();
                break;
            case FileStatus.TERMINATED:
                break;
            case FileStatus.OPENING:// This is a temporary state. DPS 9-Aug-2011
                break;
            default:
                System.out.println("Invalid File Status: " + status);
                break;
        }
    }

    private void setMenuStateInitial() {
        runGoAction.setEnabled(false);
        runStepAction.setEnabled(false);
        runBackstepAction.setEnabled(false);
        runResetAction.setEnabled(false);
        runStopAction.setEnabled(false);
        runPauseAction.setEnabled(false);
    }

    /*
     * Added DPS 9-Aug-2011, for newly-opened files. Retain existing Run menu state
     * (except Assemble, which is always true). Thus if there was a valid assembly
     * it is retained.
     */
    private void setMenuStateNotEdited() {
        /* Note: undo and redo are handled separately by the undo manager */
        // If assemble-all, allow previous Run menu settings to remain.
        // Otherwise, clear them out. DPS 9-Aug-2011
        if (!Globals.getSettings().getBooleanSetting(Settings.Bool.ASSEMBLE_ALL)) {
            runGoAction.setEnabled(false);
            runStepAction.setEnabled(false);
            runBackstepAction.setEnabled(false);
            runResetAction.setEnabled(false);
            runStopAction.setEnabled(false);
            runPauseAction.setEnabled(false);
        }
    }

    private void setMenuStateEditing() {
        /* Note: undo and redo are handled separately by the undo manager */
        runGoAction.setEnabled(false);
        runStepAction.setEnabled(false);
        runBackstepAction.setEnabled(false);
        runResetAction.setEnabled(false);
        runStopAction.setEnabled(false);
        runPauseAction.setEnabled(false);
    }

    /*
     * Use this when "File -> New" is used
     */
    private void setMenuStateEditingNew() {
        /* Note: undo and redo are handled separately by the undo manager */
        runGoAction.setEnabled(false);
        runStepAction.setEnabled(false);
        runBackstepAction.setEnabled(false);
        runResetAction.setEnabled(false);
        runStopAction.setEnabled(false);
        runPauseAction.setEnabled(false);
    }

    /*
     * Use this upon successful assemble or reset
     */
    private void setMenuStateRunnable() {
        /* Note: undo and redo are handled separately by the undo manager */
        runGoAction.setEnabled(true);
        runStepAction.setEnabled(true);
        runBackstepAction.setEnabled(
                Globals.getSettings().getBackSteppingEnabled() && !Globals.program.getBackStepper().empty());
        runResetAction.setEnabled(true);
        runStopAction.setEnabled(false);
        runPauseAction.setEnabled(false);
    }

    /*
     * Use this while program is running
     */
    private void setMenuStateRunning() {
        /* Note: undo and redo are handled separately by the undo manager */
        runGoAction.setEnabled(false);
        runStepAction.setEnabled(false);
        runBackstepAction.setEnabled(false);
        runResetAction.setEnabled(false);
        runStopAction.setEnabled(true);
        runPauseAction.setEnabled(true);
    }

    /*
     * Use this upon completion of execution
     */
    public void setMenuStateTerminated() {
        /* Note: undo and redo are handled separately by the undo manager */
        runGoAction.setEnabled(false);
        runStepAction.setEnabled(false);
        runBackstepAction.setEnabled(
                Globals.getSettings().getBackSteppingEnabled() && !Globals.program.getBackStepper().empty());
        runResetAction.setEnabled(true);
        runStopAction.setEnabled(false);
        runPauseAction.setEnabled(false);
    }
}
