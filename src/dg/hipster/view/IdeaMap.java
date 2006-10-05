/*
 * IdeaMap.java
 *
 * Created on August 31, 2006, 6:03 PM
 *
 * Copyright (c) 2006, David Griffiths
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of David Griffiths nor the names of his contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package dg.hipster.view;

import dg.hipster.controller.IdeaMapController;
import dg.hipster.model.Idea;
import dg.inx.XMLPanel;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JTextField;
import javax.swing.Timer;

/**
 * Main component for displaying the mind maps.
 * @author davidg
 */
public final class IdeaMap extends JComponent implements MapComponent {
    /**
     * Proportion that the image will be scaled in and out each
     * time the {@link #zoomIn()} and {@link #zoomOut()} methods are called.
     */
    public final static double SCALE_FACTOR = 1.5;
    /**
     * Controller object for this idea map.
     */
    private IdeaMapController controller;
    /**
     * Idea that will appear at the centre of the map.
     */
    private CentreView rootView;
    /**
     * Currently selected idea branch (if any).
     */
    private IdeaView selected;
    /**
     * Text field that appears at the top of the component.
     */
    private JTextField text;
    /**
     * Amount this map is scaled.
     */
    private double zoom = 1.0;
    /**
     * Amount to offset the map by.
     */
    private Point offset;
    /**
     * Index to be applied to next autogenerated idea name
     * (eg 3 for &quot;New 3&quot;).
     */
    private int newCount = 0;
    /**
     * Default background colour.
     */
    private static final Color DEFAULT_BACKGROUND = new Color(95, 95, 95);
    /**
     * Floating properties panel.
     */
    private FloatingPanel propertiesPanel;
    /**
     * Pause between animation updates. 40 = 20 fps.
     */
    private static final int REFRESH_PAUSE = 50; // milliseconds
    /**
     * Timer that updates the edited text.
     */
    private Timer ticker = new Timer(REFRESH_PAUSE, new ActionListener() {
        public void actionPerformed(final ActionEvent evt) {
            repaint();
        }
    });
    /**
     * &quot;From&quot; point of the link rubber-band.
     */
    private Point rubberBandFrom;
    /**
     * &quot;To&quot; point of the link rubber-band.
     */
    private Point rubberBandTo;
    /**
     * Whether the properties panel is visible.
     */
    private boolean propertiesVisible;


    /** Creates a new instance of Fred */
    public IdeaMap() {
        text = new JTextField("");
        setLayout(new BorderLayout());
        add(text, BorderLayout.NORTH);
        controller = new IdeaMapController(this);
        setBackground(DEFAULT_BACKGROUND);
        JLayeredPane mapArea = new JLayeredPane();
        mapArea.setBackground(new Color(0, 0, 0, 0));
        add(mapArea, BorderLayout.CENTER);
        propertiesPanel = new FloatingPanel();
        propertiesPanel.setCaption("Properties");
        propertiesPanel.setBounds(50, 50, 200, 200);
        JLabel labTest = new JLabel("Test");
        labTest.setForeground(null);
        propertiesPanel.getContentPane().add(labTest);
        mapArea.add(propertiesPanel);
        this.setPropertiesVisible(false);
    }

    /**
     * Text field that appears at the top of the component.
     * @return Text field that appears at the top of the component.
     */
    public JTextField getTextField() {
        return this.text;
    }

    /**
     * Set the central newIdea of the map.
     *
     * @param newIdea Idea that will be displayed at the centre.
     */
    public void setIdea(Idea newIdea) {
        Idea oldIdea = getIdea();
        if ((newIdea != null) && (!newIdea.equals(oldIdea))) {
            this.rootView = new CentreView(newIdea);
            this.rootView.setParent(this);
            this.selected = rootView;
            setSelected(newIdea);
            text.setText(newIdea.getText());
            text.setEnabled(false);
        }
        this.resetView();
    }

    /**
     * Currently selected idea branch (if any).
     * @return Currently selected idea branch (if any).
     */
    public Idea getSelected() {
        return this.selected.getIdea();
    }

    /**
     * Currently selected idea branch (if any).
     * @param selectedIdea Currently selected idea branch (if any).
     */
    public void setSelected(Idea selectedIdea) {
        IdeaView selectedView = findIdeaViewFor(rootView, selectedIdea);
        if (this.selected != null) {
            this.selected.setSelected(false);
        }
        this.selected = selectedView;
        if (this.selected != null) {
            this.selected.setSelected(true);
            propertiesPanel.getContentPane().removeAll();
            propertiesPanel.getContentPane().add(new XMLPanel(
                    selectedView.getIdea(),
                    "/dg/hipster/view/ideaProperties.xml"));
            propertiesPanel.auto();
            this.setPropertiesVisible(this.getPropertiesVisible());
            if (propertiesVisible) {
                propertiesPanel.setVisible(false);
                propertiesPanel.setVisible(true);
            }
            text.setText(selected.getIdea().getText());
        } else {
            propertiesPanel.setVisible(false);
            text.setText("");
        }
    }

    /**
     * Find the view (if any) that represents the given idea.
     * Start the search at the given view, and search all of
     * it's sub-views.
     * @param parentView View to start the search at.
     * @param idea idea we are looking for.
     * @return idea-view representing the idea, or null
     * if none are found.
     */
    private IdeaView findIdeaViewFor(IdeaView parentView, Idea idea) {
        if (idea == null) {
            return null;
        }
        if (parentView.getIdea().equals(idea)) {
            return parentView;
        }
        for (IdeaView subView: parentView.getSubViews()) {
            IdeaView ideaView = findIdeaViewFor(subView, idea);
            if (ideaView != null) {
                return ideaView;
            }
        }
        return null;
    }

    /**
     * Currently selected idea branch (if any).
     * @return Currently selected idea branch (if any).
     */
    public IdeaView getSelectedView() {
        return this.selected;
    }

    /**
     * The idea represented at the centre of this map.
     * @return central idea.
     */
    public Idea getIdea() {
        if (this.rootView != null) {
            return this.rootView.getIdea();
        }
        return null;
    }

    /**
     * The idea-view at the centre of this map.
     * @return idea-view at the centre of this map.
     */
    public IdeaView getRootView() {
        return this.rootView;
    }

    /**
     * Paint the map part of the component (the text-field
     * will paint itself).
     * @param gOrig Graphics object to draw on.
     */
    public void paintComponent(Graphics gOrig) {
        Dimension size = getSize();
        gOrig.setColor(this.getBackground());
        gOrig.fillRect(0, 0, size.width, size.height);
        Graphics g = gOrig.create();
        ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.BLACK);
        g.translate(size.width / 2, size.height / 2);
        if (offset != null) {
            g.translate(offset.x, offset.y);
        }
        ((Graphics2D)g).scale(zoom, zoom);
        if (rootView != null) {
            rootView.paint(g, this);
        }
        g.dispose();
        if ((rubberBandFrom != null) && (rubberBandTo != null)) {
            this.drawRubberBand((Graphics2D)gOrig);
        }
    }

    /**
     * Scale this map up by the given factor.
     * @param factor scaling factor - 1.0 for normal view.
     */
    public void zoom(double factor) {
        zoom *= factor;
        offset.x *= factor;
        offset.y *= factor;
        repaint();
    }

    /**
     * Scale this map up by {@link #SCALE_FACTOR}.
     */
    public void zoomIn() {
        zoom(SCALE_FACTOR);
    }

    /**
     * Scale this map down by {@link #SCALE_FACTOR}.
     */
    public void zoomOut() {
        zoom(1.0 / SCALE_FACTOR);
    }

    /**
     * Start adjusting the map.
     */
    public void startAdjust() {
        controller.startAdjust();
    }

    /**
     * Stop any automatic adjusting of the map.
     */
    public void stopAdjust() {
        controller.stopAdjust();
    }

    /**
     * Amount that the current display is offset.
     * @return offset point.
     */
    public Point getOffset() {
        return offset;
    }

    /**
     * Amount the current display is offset.
     * @param offset point to offset by.
     */
    public void setOffset(Point offset) {
        this.offset = offset;
        repaint();
    }

    /**
     * 2D point in map space corresponding to the given point in screen space.
     *@param p Point in screen space.
     *@return Point2D in map space.
     */
    public Point2D getMapPoint(Point p) {
        Dimension size = getSize();
        double x = p.x - (size.width / 2);
        double y = p.y - (size.height / 2);
        if (offset != null) {
            x -= offset.x;
            y -= offset.y;
        }
        double z = zoom;
        x /= z;
        y /= z;
        return new Point2D.Double(x, y);
    }

    /**
     * Point in screen space corresponding to the given point in map space.
     *@param p Point2D in map space.
     *@return Point in screen space.
     */
    private Point getScreenPoint(Point2D p) {
        Dimension size = getSize();
        double x = p.getX();
        double y = p.getY();
        double z = zoom;
        x *= z;
        y *= z;
        if (offset != null) {
            x += offset.x;
            y += offset.y;
        }
        x += (size.width / 2);
        y += (size.height / 2);
        return new Point((int) x, (int) y);
    }

    /**
     * Reset the zoom and offset.
     */
    public void resetView() {
        centreView();
        resetZoom();
        setPropertiesVisible(false);
    }

    /**
     * Centre the view.
     */
    public void centreView() {
        offset = new Point(0, 0);
        repaint();
    }

    /**
     * Centre the view.
     */
    public void resetZoom() {
        offset.x /= zoom;
        offset.y /= zoom;
        zoom = 1.0;
        repaint();
    }

    /**
     * Insert a child branch to the currently selected idea (do nothing
     * if none selected).
     */
    public void insertChild() {
        final IdeaView selected = getSelectedView();
        if (selected == null) {
            return;
        }
        Idea newIdea = new Idea("New " + (newCount++));
        selected.getIdea().add(0, newIdea);
        editIdeaView(selected.getSubViews().get(0));
    }

    /**
     * Select and put the given idea-view into edit mode.
     * @param selected view to select and edit.
     */
    public void editIdeaView(final IdeaView selected) {
        setSelected(selected.getIdea());
        selected.setEditing(true);
        text.setEnabled(true);
        text.requestFocusInWindow();
        text.selectAll();
        ticker.start();
    }

    /**
     * Insert a child idea to the currently selected idea.
     */
    public void insertIdea() {
        final IdeaView selected = getSelectedView();
        if (selected == null) {
            return;
        }
        MapComponent parent = selected.getParent();
        if (!(parent instanceof IdeaView)) {
            return;
        }
        IdeaView parentView = (IdeaView) parent;
        int pos = parentView.getSubViews().indexOf(selected);
        Idea newIdea = new Idea("New " + (newCount++));
        parentView.getIdea().add(pos + 1, newIdea);
        editIdeaView(selected.getNextSibling());
    }

    /**
     * Switch the given idea view out of edit mode.
     * @param ideaView idea-view to unedit.
     */
    public void unEditIdeaView(final IdeaView ideaView) {
        ideaView.getIdea().setText(text.getText());
        ideaView.setEditing(false);
        text.select(0, 0);
        text.setEnabled(false);
        ticker.stop();
    }

    /**
     * Delete the currently selected idea (and consequently
     * its child ideas).
     */
    public void deleteSelected() {
        final IdeaView selected = getSelectedView();
        if (selected == null) {
            return;
        }
        MapComponent parent = selected.getParent();
        if (!(parent instanceof IdeaView)) {
            return;
        }
        IdeaView parentView = (IdeaView) parent;
        IdeaView nextToSelect = null;
        IdeaView nextSibling = selected.getNextSibling();
        IdeaView previousSibling = selected.getPreviousSibling();
        if (nextSibling != null) {
            nextToSelect = nextSibling;
        } else if (previousSibling != null) {
            nextToSelect = previousSibling;
        } else {
            nextToSelect = parentView;
        }
        parentView.getIdea().remove(selected.getIdea());
        setSelected(nextToSelect.getIdea());
    }

    /**
     * Turn the given branch to point at the given point.
     * @param branch idea to to drag.
     * @param screenPoint Point it will face.
     */
    public void dragBranchTo(final BranchView branch,
            final Point screenPoint) {
        Point2D p = getMapPoint(screenPoint);
        Point2D fromPoint = branch.getFromPoint();
        MapComponent parent = branch.getParent();
        if (parent instanceof CentreView) {
            CentreView centre = (CentreView) parent;
            double x = p.getX();
            x = x * centre.ROOT_RADIUS_Y / centre.ROOT_RADIUS_X;
            p.setLocation(x, p.getY());
            fromPoint = new Point2D.Double(0, 0);
        }
        double angle = getAngleBetween(fromPoint, p);
        if (parent instanceof IdeaView) {
            IdeaView parentView = (IdeaView) parent;
            angle = angle - parentView.getRealAngle();
        }
        angle = normalizeRange(angle);
        double oldAngle = branch.getIdea().getAngle();
        if (Math.abs(oldAngle - angle) < Math.PI) {
            branch.getIdea().setAngle(angle);
        }
        startAdjust();
    }

    /**
     * Put the given angle into the range -Pi to Pi.
     * @param angle angle to transform.
     * @return equivalent in the -Pi to Pi range.
     */
    private double normalizeRange(double angle) {
        while (angle < -Math.PI) {
            angle += 2 * Math.PI;
        }
        while (angle > Math.PI) {
            angle -= 2 * Math.PI;
        }
        return angle;
    }

    /**
     * The clockwise angle in radians of the line between
     * the given points. If toP is directly above fromP, the
     * angle is 0.0.
     * @param fromP start point of the line.
     * @param toP end point of the line.
     * @return clockwise angle in radians of the line.
     */
    static double getAngleBetween(final Point2D fromP, final Point2D toP) {
        double diffX = toP.getX() - fromP.getX();
        double diffY = toP.getY() - fromP.getY();
        double angle = 0.0;
        double tan = Math.abs(diffX) / Math.abs(diffY);
        angle = Math.atan(tan);
        if (diffY > 0) {
            angle = (Math.PI - angle);
        }
        if (diffX < 0) {
            angle *= -1;
        }
        return angle;
    }

    /**
     * Request that a rubber band be drawn from the mid-point
     * of the selected idea, to the given point. This will
     * not actually be drawn until some time later, when
     * this idea map is repainted.
     * @param toPoint point to draw the rubber band line to.
     */
    public void drawLinkRubberBand(final Point toPoint) {
        IdeaView selectedView = getSelectedView();
        if ((selectedView == null) || (!(selectedView instanceof BranchView))) {
            return;
        }
        BranchView branch = (BranchView) selectedView;
        Point2D fromPoint = branch.getMidPoint();
        Point fp = getScreenPoint(fromPoint);
        Graphics2D g = (Graphics2D) getGraphics();
        rubberBandFrom = fp;
        rubberBandTo = toPoint;
        repaint();
    }

    /**
     * Request that the rubber-band line is removed. It will
     * not actually be cleared until this idea-map is repainted.
     */
    public void clearRubberBand() {
        this.rubberBandFrom = null;
        this.rubberBandTo = null;
    }

    /**
     * Draw a rubber band as specified by drawLinkRubberBand.
     * May do nothing if no band specified.
     * @param g graphics to draw on.
     */
    private void drawRubberBand(final Graphics2D g) {
        g.setColor(Color.GRAY);
        Dimension size = getSize();
        Stroke oldStroke = g.getStroke();
        float strokeWidth = BranchView.DEFAULT_STROKE_WIDTH;
        Stroke stroke = new BasicStroke(strokeWidth,
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL);
        g.setStroke(stroke);
        g.drawLine(rubberBandFrom.x, rubberBandFrom.y,
                rubberBandTo.x, rubberBandTo.y);
        g.setStroke(oldStroke);
    }

    /**
     * Whether the properties panel is visible.
     * @param show true if visible, false otherwise.
     */
    private void setPropertiesVisible(boolean show) {
        if (this.getSelectedView() != null) {
            propertiesPanel.setVisible(show);
        }
        propertiesVisible = show;
    }

    /**
     * Whether the properties panel is visible.
     * @return true if visible, false otherwise.
     */
    private boolean getPropertiesVisible() {
        return this.propertiesVisible;
    }

    /**
     * The properties visible if it is not, or vice versa.
     */
    void togglePropertiesPanel() {
        setPropertiesVisible(!getPropertiesVisible());
    }
}
