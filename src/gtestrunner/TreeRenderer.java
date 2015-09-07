package gtestrunner;

import sun.swing.DefaultLookup;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

class TreeRenderer extends JPanel implements TreeCellRenderer
{
    private int _panelPreferredWidth = 0;
    private GridLayout _gridLayout = new GridLayout(1, 2);

    protected boolean _selected;
    protected boolean _hasFocus;
    /** True if draws focus border around icon as well. */
    private boolean _drawsFocusBorderAroundIcon;
    /** If true, a dashed line is drawn as the focus indicator. */
    private boolean _drawDashedFocusIndicator;
    // If drawDashedFocusIndicator is true, the following are used.
    /**
     * Background color of the tree.
     */
    private Color _treeBGColor;
    /**
     * Color to draw the focus indicator in, determined from the background.
     * color.
     */
    private Color _focusBGColor;

    private boolean _isDropCell;
    private boolean _fillBackground;

    // Icons
    /** Icon used to show non-leaf nodes that aren't expanded. */
    protected Icon _closedIcon;

    /** Icon used to show leaf nodes. */
    protected Icon _leafIcon;

    /** Icon used to show non-leaf nodes that are expanded. */
    protected Icon _openIcon;

    // Colors
    /** Color to use for the foreground for selected nodes. */
    protected Color _textSelectionColor;

    /** Color to use for the foreground for non-selected nodes. */
    protected Color _textNonSelectionColor;

    /** Color to use for the background when a node is selected. */
    protected Color _backgroundSelectionColor;

    /** Color to use for the background when the node isn't selected. */
    protected Color _backgroundNonSelectionColor;

    /** Color to use for the focus indicator when the node has focus. */
    protected Color _borderSelectionColor;

    private JLabel _mainLabel = new JLabel();
    private JLabel _rightLabel = new JLabel();

    TreeRenderer()
    {
        this.setOpaque(false);
        this.add(_mainLabel);
        this.add(_rightLabel);

        this.setLayout(_gridLayout);
        _rightLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        setLeafIcon(UIManager.getIcon("Tree.leafIcon"));
        setClosedIcon(UIManager.getIcon("Tree.closedIcon"));
        setOpenIcon(UIManager.getIcon("Tree.openIcon"));

        setTextSelectionColor(UIManager.getColor("Tree.selectionForeground"));
        setTextNonSelectionColor(UIManager.getColor("Tree.textForeground"));
        setBackgroundSelectionColor(UIManager.getColor("Tree.selectionBackground"));
        setBackgroundNonSelectionColor(UIManager.getColor("Tree.textBackground"));
        setBorderSelectionColor(UIManager.getColor("Tree.selectionBorderColor"));

        _fillBackground = DefaultLookup.getBoolean(this, ui, "Tree.rendererFillBackground", true);
        Insets margins = DefaultLookup.getInsets(this, ui, "Tree.rendererMargins");
        if (margins != null)
        {
            setBorder(new EmptyBorder(margins.top, margins.left, margins.bottom, margins.right));
        }

        _drawsFocusBorderAroundIcon = UIManager.getBoolean("Tree.drawsFocusBorderAroundIcon");
        _drawDashedFocusIndicator = UIManager.getBoolean("Tree.drawDashedFocusIndicator");
    }

    public JLabel getLabel()
    {
        return _mainLabel;
    }

    public JLabel getRightLabel()
    {
        return _rightLabel;
    }

    @Override
    public Dimension getPreferredSize()
    {
        Dimension size = super.getPreferredSize();
        size.width = _panelPreferredWidth;
        return size;
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree,
                                                  Object value,
                                                  boolean selected,
                                                  boolean expanded,
                                                  boolean leaf,
                                                  int row,
                                                  boolean hasFocus)
    {
        String stringValue = tree.convertValueToText(value, selected, expanded, leaf, row, hasFocus);
        _mainLabel.setText(stringValue);
        _rightLabel.setText("");
        _hasFocus = hasFocus;

        Color fg = null;
        _isDropCell = false;

        JTree.DropLocation dropLocation = tree.getDropLocation();
        if (dropLocation != null
                && dropLocation.getChildIndex() == -1
                && tree.getRowForPath(dropLocation.getPath()) == row)
        {
            Color col = UIManager.getColor("Tree.dropCellForeground");
            if (col != null)
            {
                fg = col;
            }
            else
            {
                fg = getTextSelectionColor();
            }

            _isDropCell = true;
        }
        else if (selected)
        {
            fg = getTextSelectionColor();
        }
        else
        {
            fg = getTextNonSelectionColor();
        }

        this.setForeground(fg);

        Icon icon = null;
        if (leaf)
        {
            icon = getLeafIcon();
        }
        else if (expanded)
        {
            icon = getOpenIcon();
        }
        else
        {
            icon = getClosedIcon();
        }

        if (!tree.isEnabled())
        {
            this.setEnabled(false);
            LookAndFeel laf = UIManager.getLookAndFeel();
            Icon disabledIcon = laf.getDisabledIcon(tree, icon);
            if (disabledIcon != null) icon = disabledIcon;
            _mainLabel.setDisabledIcon(icon);
        }
        else
        {
            this.setEnabled(true);
            _mainLabel.setIcon(icon);
        }
        _mainLabel.setComponentOrientation(tree.getComponentOrientation());


        // TODO replace the hard-coded number with calculations
        _panelPreferredWidth = tree.getWidth() - 50;

        //_rightLabel.setText(tree.getWidth() + "");

        _selected = selected;

        return this;
    }

    @Override
    public void paint(Graphics g)
    {
        Color bColor;

        if (_isDropCell)
        {
            bColor = UIManager.getColor("Tree.dropCellBackground");
            if (bColor == null)
            {
                bColor = getBackgroundSelectionColor();
            }
        }
        else if (_selected)
        {
            bColor = getBackgroundSelectionColor();
        }
        else
        {
            bColor = getBackgroundNonSelectionColor();
            if (bColor == null)
            {
                bColor = getBackground();
            }
        }

        int imageOffset = -1;
        if (bColor != null && _fillBackground)
        {
            imageOffset = getLabelStart();
            g.setColor(bColor);
            if(getComponentOrientation().isLeftToRight())
            {
                g.fillRect(imageOffset, 0, getWidth() - imageOffset, getHeight());
            }
            else
            {
                g.fillRect(0, 0, getWidth() - imageOffset, getHeight());
            }
        }

        if (_hasFocus)
        {
            if (_drawsFocusBorderAroundIcon)
            {
                imageOffset = 0;
            }
            else if (imageOffset == -1)
            {
                imageOffset = getLabelStart();
            }
            if(getComponentOrientation().isLeftToRight())
            {
                paintFocus(g, imageOffset, 0, getWidth() - imageOffset, getHeight(), bColor);
            }
            else
            {
                paintFocus(g, 0, 0, getWidth() - imageOffset, getHeight(), bColor);
            }
        }
        super.paint(g);
    }

    /**
     * Sets the icon used to represent non-leaf nodes that are expanded.
     */
    public void setOpenIcon(Icon newIcon) {
        _openIcon = newIcon;
    }

    /**
     * Returns the icon used to represent non-leaf nodes that are expanded.
     */
    public Icon getOpenIcon() {
        return _openIcon;
    }

    /**
     * Sets the icon used to represent non-leaf nodes that are not expanded.
     */
    public void setClosedIcon(Icon newIcon) {
        _closedIcon = newIcon;
    }

    /**
     * Returns the icon used to represent non-leaf nodes that are not
     * expanded.
     */
    public Icon getClosedIcon() {
        return _closedIcon;
    }

    /**
     * Sets the icon used to represent leaf nodes.
     */
    public void setLeafIcon(Icon newIcon) {
        _leafIcon = newIcon;
    }

    /**
     * Returns the icon used to represent leaf nodes.
     */
    public Icon getLeafIcon() {
        return _leafIcon;
    }
    /**
     * Sets the color the text is drawn with when the node is selected.
     */
    public void setTextSelectionColor(Color newColor) {
        _textSelectionColor = newColor;
    }

    /**
     * Returns the color the text is drawn with when the node is selected.
     */
    public Color getTextSelectionColor() {
        return _textSelectionColor;
    }

    /**
     * Sets the color the text is drawn with when the node isn't selected.
     */
    public void setTextNonSelectionColor(Color newColor) {
        _textNonSelectionColor = newColor;
    }

    /**
     * Returns the color the text is drawn with when the node isn't selected.
     */
    public Color getTextNonSelectionColor() {
        return _textNonSelectionColor;
    }

    /**
     * Sets the color to use for the background if node is selected.
     */
    public void setBackgroundSelectionColor(Color newColor) {
        _backgroundSelectionColor = newColor;
    }


    /**
     * Returns the color to use for the background if node is selected.
     */
    public Color getBackgroundSelectionColor() {
        return _backgroundSelectionColor;
    }

    /**
     * Sets the background color to be used for non selected nodes.
     */
    public void setBackgroundNonSelectionColor(Color newColor) {
        _backgroundNonSelectionColor = newColor;
    }

    /**
     * Returns the background color to be used for non selected nodes.
     */
    public Color getBackgroundNonSelectionColor() {
        return _backgroundNonSelectionColor;
    }

    /**
     * Sets the color to use for the border.
     */
    public void setBorderSelectionColor(Color newColor) {
        _borderSelectionColor = newColor;
    }

    /**
     * Returns the color the border is drawn.
     */
    public Color getBorderSelectionColor() {
        return _borderSelectionColor;
    }

    private void paintFocus(Graphics g, int x, int y, int w, int h, Color notColor)
    {
        Color bsColor = getBorderSelectionColor();

        if (bsColor != null && (_selected || !_drawDashedFocusIndicator))
        {
            g.setColor(bsColor);
            g.drawRect(x, y, w - 1, h - 1);
        }
        if (_drawDashedFocusIndicator && notColor != null)
        {
            if (_treeBGColor != notColor)
            {
                _treeBGColor = notColor;
                _focusBGColor = new Color(~notColor.getRGB());
            }
            g.setColor(_focusBGColor);
            BasicGraphicsUtils.drawDashedRect(g, x, y, w, h);
        }
    }

    private int getLabelStart()
    {
        Icon currentI = _mainLabel.getIcon();
        if(currentI != null && _mainLabel.getText() != null)
        {
            return currentI.getIconWidth() + Math.max(0, _mainLabel.getIconTextGap() - 1);
        }
        return 0;
    }
}