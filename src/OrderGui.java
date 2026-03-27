import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/**
 * The centralised GUI controller class.
 * <p>Coordinates views, as well as interface between view input panels and back-end MenuSearcher.
 * <p>Acts as listener for events from child panels and from the model core (MenuSearcher)</p>
 *
 * Images created by ChatGPT5.
 */
public final class OrderGui implements OrderingSystemListener, ResultsPanelListener, OrderCreationPanelListener {
    private final JFrame frame;
    private final CardLayout topCardLayout = new CardLayout();
    private final JPanel topCardPanel = new JPanel(topCardLayout);

    // The core Panel where filter selects are actually situated and operated.
    private final FilterEntryPanel filterEntryPanel;
    private final ResultsPanel resultsPanel;
    private final OrderCreationPanel orderCreationPanel;

    // Store subscribers to the GuiListener. Currently only intended to be MenuSearcher--so
    // Collection not strictly needed, but it's reasonable this could expand in the future.
    private final List<GuiListener> listeners = new ArrayList<>();

    //Store the cheese selected at search to pass to the OrderCreationPanel (and ultimately MenuSearcher).
    //This could be made more abstract by storing the full FilterSelections record, but then it's
    //storing currently superfluous information and requires more explicit unpacking logic on the
    //other end.
    //This is the only clean way to pass this information, as the relevant panels are all
    //instantiated by the OrderGui constructor for reusability.
    //Pre-cast to String to reduce exposure by non-final field.
    private String lastSearchedCheese;

    //                              ***CONSTANTS***
    private static final Dimension GUI_PREFERRED_SIZE = new Dimension(1024, 576);
    private static final String SEARCH_BUTTON_IMG_PATH = "./resources/search-now.png";
    private static final String WELCOME_BACKGROUND_IMG_PATH = "./resources/welcome-background.png";
    private static final String SIDE_BANNER_IMG_PATH = "./resources/side_banner.png";

    //IMAGES
    private static final BufferedImage FRAME_ICON_IMAGE =
            ImgAndButtonUtilities.loadBufferedImage("./resources/overloaded_burgers_graphic_small.png");
    private static final ImageIcon SUCCESS_ICON = new ImageIcon(
            ImgAndButtonUtilities.loadBufferedImage("./resources/success_icon.png"));
    private static final ImageIcon FAIL_ICON = new ImageIcon(
            ImgAndButtonUtilities.loadBufferedImage("./resources/fail_icon.png"));


    /**
     * Constructor for the main GUI for OverLoaded Burgers ordering system.
     * <p> Initialises the main frame, instantiates all major view panels,
     * sets itself as the listener for those panels, and composes the final UI.
     *
     * @param filterOptions Map of available options for each Filter, used to
     *                      populate the FilterEntryPanel selectors based on read-in menu data.
     */
    public OrderGui(Map<Filter, List<Object>> filterOptions) {
        //CREATE VIEW PANELS
        this.filterEntryPanel = new FilterEntryPanel(filterOptions);

        this.resultsPanel = new ResultsPanel();
        this.resultsPanel.setResultsPanelListener(this); //register as listener

        this.orderCreationPanel = new OrderCreationPanel();
        this.orderCreationPanel.setPersonalDetailsListener(this); //register as listener

        //SETUP MAIN FRAME
        frame = new JFrame("Overloaded Burgers");
        frame.setIconImage(FRAME_ICON_IMAGE);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setPreferredSize(GUI_PREFERRED_SIZE);
        frame.setLayout(new BorderLayout()); //allow responsive resize.
        frame.setResizable(true);

        //FINISH PUTTING TOGETHER THE MAIN VIEW
        composeTopCardPanel();
        frame.add(topCardPanel, BorderLayout.CENTER);

        frame.pack();
        frame.setLocationRelativeTo(null); //centre on screen; must be after pack but before set visible
        frame.setVisible(true);
    }

    /**
     * Compose the top-level Panel that holds all other views managed by the CardLayout. Give each view a name.
     */
    private void composeTopCardPanel(){
        topCardPanel.add(makeWelcomeCardPanel(), "welcomePanel");
        topCardPanel.add(makeMainBurgerFilterPanel(), "mainFilterPanel");
        topCardPanel.add(resultsPanel.getCorePanel(), "resultsPanel");
        topCardPanel.add(orderCreationPanel.getCorePanel(), "orderCreationPanel");
    }

    /**
     * Factory helper that creates the welcome screen Panel.
     * <p>Has a responsive background image and an "Order Now" button to proceed to the main filter view.
     *
     * @return JPanel for the welcome card
     */
    private JPanel makeWelcomeCardPanel() {
        //LOCAL CONSTANTS FOR LAYOUT MANAGEMENT
        int WELCOME_CARD_ROWS = 8;
        int WELCOME_CARD_COLS = 5;

        Image backgroundImage = ImgAndButtonUtilities.loadBufferedImage(WELCOME_BACKGROUND_IMG_PATH);

        //CREATE THE CORE PANEL; custom repaint logic for responsive resize of direct-painted background img
        JPanel welcomePanel = new JPanel(new GridLayout(WELCOME_CARD_ROWS, 1)) {
            //Custom repaint to paint directly to background, allowing responsive resize.
            //Code ideas as cited in ImgAndButtonUtilities responsive image methods.
            @Override protected void paintComponent (Graphics g) {
                // Clear background and set-up non-custom aspects of the JPanel
                // https://docs.oracle.com/javase/tutorial/uiswing/painting/closer.html
                super.paintComponent(g);

                // Set Rendering hints to make it scale nicely
                Graphics2D g2d = (Graphics2D) g.create();

                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);

                //Actually draw it!
                g2d.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);

                g2d.dispose();
            }
        };

        welcomePanel.setPreferredSize(GUI_PREFERRED_SIZE);//Preload with logical preferred size

        //CREATE THE 'Order Now' BUTTON AND ADD ACTION LISTENER
        JButton orderNow = ImgAndButtonUtilities.makeImgOnlyButtonWithResize(
                "./resources/order-now-button.png",
                new Dimension((int)(GUI_PREFERRED_SIZE.width / WELCOME_CARD_COLS),
                        (int)(GUI_PREFERRED_SIZE.height / WELCOME_CARD_ROWS))
        );
        orderNow.setBorderPainted(false); //get rid of the default rectangular border
        orderNow.addActionListener(e -> switchCard("mainFilterPanel"));


        //                      *** LAYOUT MANAGEMENT ***

        //POSITION BUTTON AT THE BASE OF THE GRIDBAGLAYOUT USING THE INVISIBLE FILLER TO SQUISH IT INTO POSITION.
        //Button horizontally centred and vertically bottom row.

        //Create blank labels that occupy all but the last row of the parent's grid layout
        JLabel[] blankRowLabels = new JLabel[WELCOME_CARD_ROWS-1];
        for (int i = 0; i < WELCOME_CARD_ROWS-1; i++) {
            blankRowLabels[i] = new JLabel();
            blankRowLabels[i].setOpaque(false);
            welcomePanel.add(blankRowLabels[i]);
        }


        //Arrange the final row in a self-contained Panel to center the button
        JPanel centredButtonPanel = new JPanel(new GridLayout(1, WELCOME_CARD_COLS));
        centredButtonPanel.setOpaque(false);
        // Make an array of invisible JLabels; number 0 to push the button down to the south, and 1
        // and 2 to horizontally centre it on its row.
        JLabel[] blankColLabels = new JLabel[WELCOME_CARD_COLS];
        for (int i = 0; i < blankColLabels.length; i++) {
            blankColLabels[i] = new JLabel();
            blankColLabels[i].setOpaque(false);
            if (i == 2) {
                centredButtonPanel.add(orderNow);
            } else {
                centredButtonPanel.add(blankColLabels[i]);
            }
        }

        welcomePanel.add(centredButtonPanel);

        return welcomePanel;
    }

    /**
     * Creates the main filtering view; composes core FilterEntryPanel onto a Panel with a side banner and search button.
     * <p>Nested GridBagLayouts for split.
     * <p>Side banner image responsively resizes (code attributions as for buttons and other responsive resizing images)
     * @return JPanel with the mainBurgerFilterPanel
     */
    private JPanel makeMainBurgerFilterPanel() {
        // SIDE BANNER - ALWAYS VISIBLE
        //Create image with custom scaling to fit the width of its container while keeping its ratio.
        final BufferedImage sourceImage = ImgAndButtonUtilities.loadBufferedImage(SIDE_BANNER_IMG_PATH);
        JLabel sideBanner = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                //Responsive resizing image and rendering hint ideas as for welcome button and welcome background jpanel
                super.paintComponent(g);

                // Set Rendering hints to make it scale nicely
                Graphics2D g2d = (Graphics2D) g.create();

                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);

                // Scale by width--looks less weird to pad top and bottom than sides.
                float scale = (float) getWidth() / sourceImage.getWidth();
                int scaledHeight = (int) (sourceImage.getHeight() * scale);
                int y = (getHeight() - scaledHeight) / 2; //Figure out the origin y coordinate.

                //Actually draw it
                g2d.drawImage(sourceImage, 0, y, getWidth(), scaledHeight, this);

                g2d.dispose();
            }
        };

        JButton searchButton = makeSearchButton();
        JPanel filterPanel = this.filterEntryPanel.getCorePanel(); //local variable name to make it neater below.

        //LOCAL CONSTANTS FOR LAYOUT WEIGHTING
        double FILTER_PANEL_Y_WEIGHT = 0.85;
        double SEARCH_BUTTON_Y_WEIGHT = 0.15;
        double SIDE_BANNER_X_WEIGHT = 0.3;
        double RIGHT_PANEL_X_WEIGHT = 0.7;

        //SET MINIMUM DIMENSIONS - token values so that layout manager respects the resize weights.
        //Otherwise sideBanner gets squished.
        int minSideBannerWidth = (int)(GUI_PREFERRED_SIZE.width * SIDE_BANNER_X_WEIGHT * 0.5);
        int minRightPanelWidth = (int)(GUI_PREFERRED_SIZE.width * RIGHT_PANEL_X_WEIGHT * 0.5);

        sideBanner.setMinimumSize(new Dimension(minSideBannerWidth, 1));
        sideBanner.setPreferredSize(new Dimension(
                (int)(GUI_PREFERRED_SIZE.width * SIDE_BANNER_X_WEIGHT),
                GUI_PREFERRED_SIZE.height
        ));
        filterPanel.setMinimumSize(new Dimension(minRightPanelWidth, 1));
        searchButton.setMinimumSize(new Dimension(1,1));

        //USE NESTED GRIDBAGLAYOUTS FOR WEIGHTED ROWS/COLUMNS
        //RIGHT SIDE (VERTICALLY STACKED)
        JPanel rightPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbcRight = new GridBagConstraints();
        gbcRight.fill = GridBagConstraints.BOTH;
        gbcRight.gridx = 0;
        gbcRight.weightx = 1.0;

        //Filter Panel - top chunk
        gbcRight.gridy = 0;
        gbcRight.weighty =  FILTER_PANEL_Y_WEIGHT;
        rightPanel.add(filterPanel, gbcRight);

        //Search Button - bottom chunk
        gbcRight.gridy = 1;
        gbcRight.weighty = SEARCH_BUTTON_Y_WEIGHT;
        rightPanel.add(searchButton, gbcRight);

        //COMPOSED MAIN PANEL - horizontal split
        JPanel mainBurgerFilterPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbcMain = new  GridBagConstraints();
        gbcMain.fill = GridBagConstraints.BOTH;
        gbcMain.gridy = 0;
        gbcMain.weighty = 1.0;

        //Side banner - left chunk
        gbcMain.gridx = 0;
        gbcMain.weightx = SIDE_BANNER_X_WEIGHT;
        mainBurgerFilterPanel.add(sideBanner, gbcMain);

        //Right panel - right chunk (i.e. filters + search)
        gbcMain.gridx = 1;
        gbcMain.weightx = RIGHT_PANEL_X_WEIGHT;
        mainBurgerFilterPanel.add(rightPanel, gbcMain);

        mainBurgerFilterPanel.setPreferredSize(GUI_PREFERRED_SIZE);

        return mainBurgerFilterPanel;
    }

    /**
     * Factory helper to make a search button with an image that responsively resizes.
     * <p>Preferred size (main frame height /10).
     * <p>Has action listener attached to perform search on click.
     * @return JButton for searching
     */
    private JButton makeSearchButton() {
        JButton searchButton = ImgAndButtonUtilities.makeImgOnlyButtonWithResize(
                SEARCH_BUTTON_IMG_PATH,
                new Dimension(0, (int)(GUI_PREFERRED_SIZE.height/10))
        );
        searchButton.addActionListener(e -> performSearch());
        searchButton.setBorderPainted(false); //get rid of the default rectangular border

        return searchButton;
    }

    /**
     * Collects user input from the filter Panel, validates it, and initiates a search.
     * <p>Action handler for the main search button.
     * <p>Validation checks include:
     * <li>All required filter selections have been made.
     * <li>Price fields contain valid, non-negative numbers.
     * <li>Max price exceeds Min price input.
     * <p>If all checks pass, creates a DreamMenuItem with the user selections and
     * passes the search request to the registered GuiListener
     * (currently MenuSearcher, though potentially this scope could expand).
     */
    private void performSearch() {

        //First check that all type-relevant filters had a selection
        String missingFilterPanelSelections = this.filterEntryPanel.getMissingSelectionsMessage();
        if (!missingFilterPanelSelections.isBlank()) {
            JOptionPane.showMessageDialog(frame, missingFilterPanelSelections,
                    "Incomplete Selections", JOptionPane.WARNING_MESSAGE);
            return; //short-circuit
        }

        //Get raw data from the core view panel
        FilterSelections selections = filterEntryPanel.getFilterSelections();

        String minPriceRaw = selections.minPrice();
        String maxPriceRaw = selections.maxPrice();

        // VALIDATE PRICE INPUT FIELDS
        if (!InputValidators.isValidPrice(minPriceRaw) || !InputValidators.isValidPrice(maxPriceRaw)) {
            JOptionPane.showMessageDialog(
                    frame, InputValidators.ERROR_INVALID_PRICE_FORMAT, "Price Input Error", JOptionPane.ERROR_MESSAGE);
            return; //Early terminate the search.
        }
        // Safely parse the known-valid prices
        double minPrice = Double.parseDouble(minPriceRaw);
        double maxPrice = Double.parseDouble(maxPriceRaw);

        //Check this explicitly here rather than by calling InputValidators as it's a particular
        //business rule rather than an objective external standard.
        if (!(maxPrice>minPrice)) {
            JOptionPane.showMessageDialog(
                    frame, "Max. Price must be higher than Min. Price.", "Price Input Error", JOptionPane.ERROR_MESSAGE);
            return; //Early terminate the search.
        }

        //Assign a string representation of the selected cheese Object. Every Object has a
        //toString(), so this can't break anything serious.
        this.lastSearchedCheese = selections.selectedCheese().toString();

        DreamMenuItem dreamMenuItem =
                new DreamMenuItem(
                        buildFilterMapFromRecord(selections), minPrice, maxPrice);

        //GUI's done processing logic for now--back to being a view--pass off to the relevant listener (MenuSearcher).
        for (GuiListener listener : listeners) {
            listener.performSearch(dreamMenuItem);
        }
    }


    /**
     * Private helper to translate FilterSelections Record to the required format for search by DreamMenuItem.
     * <p>Calls on its own helpers for tidiness.</p>
     * <p>Contains processing logic for empty and null selections; null (i.e. 'Skip-Any will do') selections are not added.
     * Selections irrelevant to the type are also not added--type relevance informed by Filter Smart Enum.
     * @param selections FilterSelections Record of selections by user.
     * @return immutable and not-null Map containing all non-skipped selections,
     * or SpecialChoice.NONE if they were deliberate NONE selections.
     */
    private Map<Filter, Object> buildFilterMapFromRecord(FilterSelections selections) {
        Map<Filter, Object> filterMap = new HashMap<>();

        Type selectedType = selections.selectedType(); //local var to avoid isRelevant lookup

        for (Filter filter : Filter.values()) {
            boolean isRelevant =
                    (selectedType == Type.BURGER && filter.isRelevantForBurger()) ||
                            (selectedType == Type.SALAD && filter.isRelevantForSalad());
            if (isRelevant) {
                Object value = getFilterValue(filter, selections);
                if (value != null) {
                    filterMap.put(filter, value);
                }
            }
        }
        return Map.copyOf(filterMap);
    }

    /**
     * Associates FilterSelections Record to Filter values.
     * @param filter the Filter
     * @param selections the FilterSelections record
     * @return an Object superclass equivalent of the relevant value within the Record
     */
    private Object getRawFilterValue(Filter filter, FilterSelections selections) {
        return switch (filter) {
            case TYPE -> selections.selectedType();
            case BUN -> selections.selectedBun();
            case SAUCES -> selections.selectedSauces();
            case DRESSING -> selections.selectedDressing();
            case LEAFY_GREENS -> selections.selectedLeafyGreens();
            case PROTEIN ->  selections.selectedProteins();
            case TOMATO -> selections.tomatoSelection();
            case CUCUMBER -> selections.cucumberSelection();
            case PICKLES -> selections.pickleSelection();
            case CHEESE -> selections.selectedCheese();
            default -> throw new IllegalArgumentException("Unexpected filter at getRawFilterValue: " + filter);
        };
    }

    /**
     * Derives a meaningful value from a FilterSelections record for a particular filter.
     * <p> Helps distinguish regular choices from special choices (i.e. 'NONE' or 'I_DONT_MIND')
     * and unpack them from Collections if necessary.
     * @param filter the Filter being checked
     * @param selections the FilterSelections Record
     * @return <li>null if it was intended to be skipped, <li>SpecialChoice.NONE if it was an intended NONE choice,
     * <li>or the full Collection or original object if it didn't contain either special choice.
     */
    private Object getFilterValue(Filter filter, FilterSelections selections) {
        Object value = getRawFilterValue(filter, selections);
        if (value == null) return null;

        if (value instanceof Collection<?> collectedValues) {
            if (filter.allowsDontMindChoice() && collectedValues.contains(filter.getDontMindValue())) {
                return null; //Don't filter based on this.
            }

            if (filter.allowsNoneChoice() && collectedValues.contains(SpecialChoice.NONE)) {
                return SpecialChoice.NONE;
            }

            return collectedValues; //Obviously a normal selection--return it as is
        } else {
            //Now do non-collection objects--i.e. single choice-only selectors
            if (filter.allowsDontMindChoice() && value.equals(filter.getDontMindValue())) {
                return null; //Don't filter based on this 'don't mind' choice
            }

            if  (filter.allowsNoneChoice() && value.equals(SpecialChoice.NONE)) {
                return SpecialChoice.NONE;
            }

            return value; //obviously a normal selection--return it.
        }
    }

    /**
     * Resets the GUI to its initial state after a successful order.
     * <p>Clears all input fields of various panels and returns to the welcome screen.
     */
    private void resetForNewOrder() {
        orderCreationPanel.clearFields();
        filterEntryPanel.clearSelections();
        //NB. ResultsPanel clears itself on use due to simpler functionality.
        switchCard("welcomePanel"); //Switch back to welcome panel after a successful order
    }


    /**
     * Helper to switch the current main card JPanel in the GUI.
     * @param cardName String of the card name to display
     */
    private void switchCard(String cardName) {
        topCardLayout.show(topCardPanel, cardName);
    }

    //              ***LISTENER INTERFACE INTERACTION METHODS***

    /**
     * Registers a listener (expected MenuSearcher) to be alerted of core user actions that
     * require processing by model, like searches or order submissions.
     *
     * @param listener the GuiListener to add
     */
    public void addGuiListener(GuiListener listener) {this.listeners.add(listener);}

    /**
     * Shows the search results in the ResultsPanel and switches to that view.
     * @param matches a List of the MenuItems that matched.
     */
    @Override
    public void onSearchResults(List<MenuItem> matches) {
        resultsPanel.displayItems(matches, "You've matched! Here are your results:");
        switchCard("resultsPanel");
    }

    /**
     * Switches to the ResultsPanel view.
     * Informs the user that no matches were found and shows the full menu as a default fallback.
     * @param fullMenu List of all MenuItems
     */
    @Override
    public void onNoMatchesFound(List<MenuItem> fullMenu) {
        String title = "Sorry, no matches were found. Here is our full menu:";
        resultsPanel.displayItems(fullMenu, title);
        switchCard("resultsPanel");
    }

    /**
     * Clears all selections on the filterEntryPanel, then switches to the that view.
     */
    @Override
    public void onBackButtonPressed() {
        this.filterEntryPanel.clearSelections();
        switchCard("mainFilterPanel");
    }


    /**
     * Sends the user back to the resultsPanel from the order creation view.
     */
    @Override
    public void onBackToMenuSelection() {
        switchCard("resultsPanel"); //Invoked from personal details panel; goes back to results panel
    }

    /**
     * Performs final validation checks on the user's name and phone number.
     * <p>If valid, passes the final Order record to the GuiListener (currently MenuSearcher) for processing.
     * @param order immutable Order record containing the customer's details and order.
     */
    @Override
    public void onFinalSubmitOrder(Order order) {
        if (!InputValidators.isFullName(order.name())) {
            JOptionPane.showMessageDialog(
                    frame, InputValidators.ERROR_INVALID_NAME, "Invalid name", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!InputValidators.isValidPhoneNo(order.phoneNoAsString())) {
            JOptionPane.showMessageDialog(
                    frame, InputValidators.ERROR_INVALID_PHONE, "Invalid phone", JOptionPane.ERROR_MESSAGE);
            return;
        }

        //WRITE THE ORDER OUT TO FILE VIA INTERFACE
        for (GuiListener listener : listeners) {
            listener.submitOrder(order);
        }
    }

    /**
     * Shows a Dialog confirming the order success and resets the application for a new order.
     * @param order Order record of the successful order
     */
    @Override
    public void onOrderSubmissionSuccess(Order order) {
        JOptionPane.showMessageDialog(
                frame,
                "Order submitted successfully!\n\nName: " + order.name() + "\nPhone: " + order.phoneNoAsString(),
                "Order Confirmed",
                JOptionPane.INFORMATION_MESSAGE,
                SUCCESS_ICON);

        resetForNewOrder();
    }

    /**
     * Shows a Dialog informing the user that the order failed with the relevant error message from the source.
     * <p>Subsequently resets the application for a new order.
     * @param errorMessage String of the particular error encountered.
     */
    @Override
    public void onOrderSubmissionFailed(String errorMessage) {
        JOptionPane.showMessageDialog(
                frame,
                errorMessage,
                "Order submission failed",
                JOptionPane.ERROR_MESSAGE,
                FAIL_ICON);

        resetForNewOrder();
    }


    /**
     * Used to transition from the ResultsPanel to the OrderCreationPanel.
     * <p>Passes a List of the MenuItems selected, the cheese preferences identified at the filter view,
     * and tells OrderCreationPanel to get ready.
     * @param selectedItems List of selected MenuItems.
     */
    @Override
    public void onProceedToDetails(List<MenuItem> selectedItems) {
        orderCreationPanel.setCheeseForOrder(this.lastSearchedCheese);
        orderCreationPanel.displayOrderSummary(selectedItems); //Tell order creation panel what was ordered
        orderCreationPanel.addItemDetailsToPanel();
        switchCard("orderCreationPanel");
    }

    // PUBLIC GETTERS

    /**
     * Public static helper so any relevant class' component can find the preferred GUI size; single source of truth
     * <p>Not currently used, but so important to extendability and consistency that this is provided.</p>
     * @return copy of the Dimension of the GUI's preferred size
     */
    public static Dimension getGuiPreferredSize() {
        return new Dimension(GUI_PREFERRED_SIZE);
    }
}
