import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public final class MenuItemPanel {
    private final JPanel corePanel;
    private final JCheckBox checkBox;
    private final MenuItem menuItem;

    private static final Dimension IMG_THUMBNAIL_DIMENSION = new Dimension(100,100);
    private static final int CORE_PANEL_HEIGHT = 300;

    public MenuItemPanel(MenuItem menuItem) {
        this.menuItem = menuItem;

        //SET UP THE MAIN PANEL
        this.corePanel = new JPanel(new BorderLayout(10,0));
        this.corePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY), //Outer border
                BorderFactory.createEmptyBorder(5,5,5,5) //inner padding
        ));

        //Panel should stretch to fit its container in width, but height should just be its internal
        //image + a bit of padding. Set here so it doesn't flood the viewport.
        this.corePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, CORE_PANEL_HEIGHT));

        //SUB-COMPONENTS
        this.checkBox = new JCheckBox();

        //Pop the item's image on a label and scale it
        JLabel imageLabel = new JLabel();
        imageLabel.setPreferredSize(IMG_THUMBNAIL_DIMENSION);
        BufferedImage originalImg =
                ImgAndButtonUtilities.loadBufferedImage(
                        "./images/" + this.menuItem.getMenuItemIdentifier() + ".png"
                        );
        Image scaledImg = originalImg.getScaledInstance(
                IMG_THUMBNAIL_DIMENSION.width, IMG_THUMBNAIL_DIMENSION.height, Image.SCALE_FAST);
        imageLabel.setIcon(new ImageIcon(scaledImg));

        //The text area for information
        JEditorPane infoArea = new JEditorPane() {
            //Override to make it autofit to viewport. Frustration, and then solution, from:
            //https://stackoverflow.com/questions/280106/jeditorpane-inside-jscrollpane-not-resizing-as-needed
            @Override
            public boolean getScrollableTracksViewportWidth() {
                return true;
            }
            // The above mostly cured it, but I eventually figured that--like so many Swing
            // responsiveness issues--it was a preferred size problem.
            // It's so common that IntelliJ autocompleted the whole override! Swing, hey... Details at
            // https://docs.oracle.com/javase/6/docs/api/javax/swing/JEditorPane.html#getPreferredSize()
            @Override
            public Dimension getPreferredSize() {
                Dimension dim = super.getPreferredSize();
                dim.width = IMG_THUMBNAIL_DIMENSION.width; //token size smaller than the viewport should be
                return dim;
            }
        };
        infoArea.setEditable(false);
        infoArea.setContentType("text/html");
        //make sure it matches the background--easier to set global styles
        infoArea.setBackground(this.corePanel.getBackground());

        //Tell the JEditorPane to respect the current look and feel, rather than forcing its HTML-editor styling.
        // Idea from https://stackoverflow.com/questions/12542733/setting-default-font-in-jeditorpane
        infoArea.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        infoArea.setFont(UIManager.getFont("Label.font"));

        infoArea.setText(menuItemInfoFormatted());
        infoArea.setCaretPosition(0); //JEditorPane weirdly autoscrolls down a bit to start--start at the beginning.

        JScrollPane textScrollPane = new JScrollPane(infoArea); //Add a scrollpane in case
        textScrollPane.setBorder(null); //get rid of the yucky default scrollpane border

        //LAYOUT
        //Pop everything in a sub-panel to aid resizing
        JPanel contentPanel = new JPanel(new BorderLayout(10,0));
        contentPanel.add(imageLabel, BorderLayout.WEST); //Image gets its preferred size on the left
        contentPanel.add(textScrollPane, BorderLayout.CENTER); //Text gets the rest of the space

        //Add the checkbox and the content panel back into the corePanel
        this.corePanel.add(this.checkBox, BorderLayout.WEST);
        this.corePanel.add(contentPanel, BorderLayout.CENTER);
    }

    private String menuItemInfoFormatted() {
        //I really wanted to BOLD some bits of the text, but JTextArea wouldn't support it, and
        //JLabel wouldn't allow all the nice auto format methods of the former. So I went down this
        //rabbit-hole of HTML formatting in a JEditorPane:
        //https://stackoverflow.com/questions/5915061/html-in-jtextarea-of-jeditorpane-jtextpane?noredirect=1&lq=1
        //https://www.geeksforgeeks.org/java/java-jeditorpane/
        //I wonder why I'm time poor...

        StringBuilder sb = new StringBuilder("<html>");

        sb.append("<b>")
                .append(this.menuItem.getMenuItemName())
                .append(" (")
                .append(this.menuItem.getMenuItemIdentifier())
                .append(") ")
                .append("</b><br>");

        sb.append(this.menuItem.getDescription())
                .append("<br>");

        String dreamInfoHtml = this.menuItem.getDreamMenuItemInfo().replace("\n", "<br>");
        sb.append(dreamInfoHtml);

        sb.append("</html>");

        //Returns the html-ified String, without price if it was a custom item (as in original DreamMenuItem logic).
        return sb.toString();
    }

    public boolean isSelected() {return this.checkBox.isSelected();}

    public MenuItem getMenuItem() {return menuItem;}

    public JPanel getCorePanel() {return this.corePanel;}
}
