/********************************************************************************/
/*										*/
/*		BudaConstants.java						*/
/*										*/
/*	BUblles Display Area constants						*/
/*										*/
/********************************************************************************/
/*	Copyright 2009 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.				 *
 *										 *
 *			  All Rights Reserved					 *
 *										 *
 * This program and the accompanying materials are made available under the	 *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at								 *
 *	http://www.eclipse.org/legal/epl-v10.html				 *
 *										 *
 ********************************************************************************/


/* SVN: $Id$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.buda;

import edu.brown.cs.bubbles.board.BoardFont;
import edu.brown.cs.bubbles.board.BoardProperties;

import org.w3c.dom.Element;

import javax.swing.JComponent;

import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.EventListener;


/**
 *	This class holds a variety of enumeration types and constants that define
 *	the basic properties of the bubble display.  It can be changed and then
 *	the system recompiled.	Most of these constants should be made into
 *	properties and read from a property file so they can be changed by the
 *	user without recompiling.
 *
 **/

public interface BudaConstants {

/**
 * Properties file
 */
static final BoardProperties BUDA_PROPERTIES = BoardProperties.getProperties("Buda");


/********************************************************************************/
/*										*/
/*	Bubble Constants							*/
/*										*/
/********************************************************************************/

/**
 *	Define the border type for a bubble.
 **/

enum BudaBorder {
   NONE,
   RECTANGLE,
   ROUNDED
}



/********************************************************************************/
/*										*/
/*	Bubble Area constants							*/
/*										*/
/********************************************************************************/

/**
 *	Height of the overview bar at the top of the display
 **/
int BUBBLE_OVERVIEW_HEIGHT = 92;		// height of the overview bar


/**
 *	Width of the virtual bubble area.  Currently this is not expandable.
 **/
int BUBBLE_DISPLAY_WIDTH = 48000;		// width of the virtual bubble area


/**
 *	Height of the virtual bubble area.  Currently this is not expandable
 **/
int BUBBLE_DISPLAY_HEIGHT = 2560;		// height of the virtual bubble area

/**
 *	X location for the initial viewport
 **/
int BUBBLE_DISPLAY_START_X= 1024;

/**
 *	Width of the initial window view.
 **/
int BUBBLE_DISPLAY_VIEW_WIDTH = 1024;

/**
 *	Height of the initial window view
 **/
int BUBBLE_DISPLAY_VIEW_HEIGHT = 600;

/**
 *	Y location of the initial viewport
 **/
int BUBBLE_DISPLAY_START_Y= ((BUBBLE_DISPLAY_HEIGHT/2)-(BUBBLE_DISPLAY_VIEW_HEIGHT/2));

/**
 *	Height of the top bar on the display
 **/
int BUBBLE_TOP_BAR_HEIGHT = 12;



/**
 *	Color at the top of the display area
 **/
String DISPLAY_TOP_COLOR_PROP = "Buda.area.top.color";


/**
 *	Color at the bottom of the display area.  The display is a gradiant from the top color
 *	to the bottom color
 **/
String DISPLAY_BOTTOM_COLOR_PROP = "Buda.area.bottom.color";



boolean DISPLAY_SHOW_GRADIENT = BUDA_PROPERTIES.getBoolean("Buda.area.gradient",false);

/**
 *	Color of the top bar on the display.  This is used for labeling working sets.
 **/
String TOP_BAR_COLOR_PROP = "Buda.topbar.color";


/**
 *	Color (top) of the background in the overview area
 **/
String OVERVIEW_TOP_COLOR_PROP = "Buda.overview.top.color";

/**
 *	Color (bottom) of the background in the overview area
 **/
String OVERVIEW_BOTTOM_COLOR_PROP = "Buda.overview.bottom.color";



/**
 *  Color for the border of bubbles displayed in the overview area when not using stylized viewport
 */
String OVERVIEW_NONSTYLIZED_VIEW_BORDER_COLOR_PROP = "Buda.OverviewNonstylizedViewBorderColor";

/**
 *	Color for the border of bubbles displayed in the overview area when using stylized viewport.
 **/
String OVERVIEW_STYLIZED_VIEW_BORDER_COLOR_PROP = "Buda.OverviewStylizedViewBorderColor";



/**
 *	Top color for the fake working set in the panning bar
 **/
String OVERVIEW_FAKE_TOP_COLOR_PROP = "Buda.OverviewFakeTopColor";

/**
 *	Bottom color for the fake working set in the panning bar
 **/
String OVERVIEW_FAKE_BOTTOM_COLOR_PROP = "Buda.OverviewFakeBottomColor";

/**
 *	Text color for the fake working set in the panning bar
 **/
String OVERVIEW_FAKE_TEXT_COLOR_PROP = "Buda.OverviewFakeTextColor";



/**
 *	Color (top) for the button panel.
 **/
String BUTTON_PANEL_TOP_COLOR_PROP = "Buda.ButtonPanelTopColor";

/**
 *	Color (bottom) for the button panel.
 **/
String BUTTON_PANEL_BOTTOM_COLOR_PROP = "Buda.ButtonPanelBottomColor";

/**
 *	Color (top) for the top bar.
 **/
String TOP_BAR_TOP_COLOR_PROP = "Buda.TopbarTopColor";

/**
 *	Color (bottom) for the top bar.
 **/
String TOP_BAR_BOTTOM_COLOR_PROP = "Buda.TopbarBottomColor";


/**
 *  Width of the border between the panels.
 */
int PANEL_BORDER_INSET	= 0;

/**
 *  Arcwidth of overview view
 */
int OVERVIEW_VIEW_ARCWIDTH = 500;

/**
 *  Archeight of overview view;
 */
int OVERVIEW_VIEW_ARCHEIGHT = 300;

/**
 * This defines whether or not the stylized viewport will be shown
 */
String OVERVIEW_STYLIZED_VIEW_BOOL = "Buda.overview.stylized.viewport";

/**
 *  This defines whether or not the "Task..." fake working set appears in the panning bar
 */
String OVERVIEW_FAKE_WS_BOOL = "Buda.show.potential.task";


/**
 *	Delay in milliseconds between automatic scrolling on dragging a window
 **/
int   AUTOSCROLL_DELAY = 10;			// delay in ms between autoscrolls


/**
 *	Delay before automatic scrolling begins
 **/
int   AUTOSCROLL_INITIAL_DELAY = 0;		// initial delay before autoscrolling starts


/**
 *	Speen in pixels per delay time for automatic scrolling.
 **/
int   AUTOSCROLL_SPEED = 40;			// speed in pixels for autoscrolling



/**
 *	Minimum amount of a bubble that must show.  This is used to restrict bubble movement
 *	so that bubbles don't move completely off the world.
 **/
int   MIN_SHOW_SIZE = 5;			// pixels that have to show



/**
 *	Delta within which a selection in the top bar must be in order to get a handle to
 *	resize a working set.
 **/
int   TOP_RESIZE_DELTA = 3;


/**
 *	Color property to overlay new bubbles with
 **/
String NEW_COLOR_PROP = "Buda.BubbleNewColor";


/**
 *	Total amount of time (in ms) that the new overlay for a bubble will show
 **/
int   NEW_BUBBLE_SHOW_TIME = 3500;


/**
 *	Amount of time between color updates for the new overay
 **/
int   NEW_BUBBLE_UPDATE_TIME = 100;


/**
 *	Minimum size in pixels of a working set
 **/
int   MIN_WORKING_SET_SIZE = 24;



/**
 *	Default minimum height/width for a bubble.  This can be overriden by individual bubbles
 *	or bubble classes.
 **/
int   BUBBLE_MIN_SIZE = 32;



/**
 *	Height of the overview bar at the top of the display for a channel
 **/
int BUBBLE_CHANNEL_OVERVIEW_HEIGHT = 50;



/**
 *	Delay in milliseconds for scroll animations
 **/
int   SCROLL_ANIM_DELAY = 10;


/**
 *	Base speed for scroll animations
 **/
int   SCROLL_ANIM_DELTA = 10;


/**
 *	Total time to be spent for automatic scrolling
 **/
int   SCROLL_ANIM_TOTAL = 500;



String USE_HELP_TOOLTIPS = "Buda.help.tooltips";



/********************************************************************************/
/*										*/
/*	Bubble Group Constants							*/
/*										*/
/********************************************************************************/

/**
 *  The width of the aura around bubbles
 */
double GROUP_AURA_BUFFER = 12;

/**
 *  The size of the bubble aura's round-rectangle arc
 */
double GROUP_AURA_ARC = 20;


/**
 *	The group area around a bubble is closest at the corners and furthest at
 *	the midpoints of the side.  This defines the close distance.
 **/
double GROUP_BORDER_NEAR = 10;		// near distance for inflated boundary


/**
 *	The group area around a bubble is closest at the corners and furthest at
 *	the midpoints of the side.  This defines the far distance.
 **/
double GROUP_BORDER_FAR = 14;		// far distance for inflated boundary



/**
 *	This defines the amount of transparency used for group shading.  The color
 *	is chosen randomly.  The transparency is in the range of 0 to 255.
 **/
int GROUP_TRANSPARENCY = 64;		// transparency for group (0-255)



/**
 *	Color on the left of a singleton group (these all have the same color).
 **/
String GROUP_SINGLE_LEFT_COLOR_PROP = "Buda.GroupSingleLeftColor";


/**
 *	Color on the right of a singleton group
 **/
String GROUP_SINGLE_RIGHT_COLOR_PROP = "Buda.GroupSingleRightColor";


/**
 *  This defines whether or not the aura color of a group will be a gradient
 */
String GROUP_DRAW_BACKGROUND_GRADIENT = "Buda.group.draw.background.gradient";




/********************************************************************************/
/*										*/
/*	Bubble Constants							*/
/*										*/
/********************************************************************************/

/**
 *	Width of the bubble border not including the actual drawn border (although
 *	part of that might be included as well.  This is only used when the bubbles
 *	are rounded.  It provides enough room between the bubble and the rounded
 *	border so that the border doesn't occlude the bubble.
 **/
int	BUDA_BORDER_WIDTH = 4;	      // when rounded


/**
 *	Size of the arc (in pixels) for rounding the borders of a bubble.
 **/
double	BUBBLE_ARC_SIZE = 8.0;		// size of arc for round rectangle


/**
 *	Width of the line used for drawing the border when not focused.
 **/
float	BUBBLE_EDGE_SIZE = 3.0f;	// border line width



String	BUDA_EDGE_SIZE_PROP = "Buda.border.width";


/**
 *	Width of the line used for drawing the border when the bubble is focused.
 **/
float	BUBBLE_FOCUS_EDGE_SIZE = 3.0f;	// border size when focused
String	BUDA_FOCUS_EDGE_SIZE_PROP = "Buda.border.focus.width";


/**
 *	Distance from the border to be used when determine if the user is attempting
 *	to resize the bubble.
 **/
int	BUBBLE_BORDER_DELTA = 5;	// what to consider the border for correlation


/**
 *	Default border color property for a non-focused bubble.
 **/
String	BUBBLE_BORDER_COLOR_PROP = "Buda.bubbleBorderColor";         // normal bubble border color


/**
 *	Default border color property for a focused bubble.
 **/
String	BUBBLE_FOCUS_COLOR_PROP = "Buda.bubbleFocusColor";           // bubble border color when it has focus


/**
 *	Distance between this bubble and a new one when the bubbles should be in the
 *	same group.
 **/
int	BUBBLE_CREATION_NEAR_SPACE = 20;	// space between bubbles for same group


/**
 *	Distance between a source bubble and a new bubble when the bubbles should be in
 *	different groups.
 **/
int	BUBBLE_CREATION_SPACE = 50;		// space between bubbles for new group



/**
 *	Background color to be used for tool tips
 **/
String	BUBBLE_TOOLTIP_COLOR_PROP = "Buda.tooltip.background";
String	BUBBLE_TOOLTIP_TEXT_PROP = "Buda.tooltip.foreground";

/**
 *	Font to be used for tool tips.
 **/
Font	BUBBLE_TOOLTIP_FONT = BoardFont.getFont(Font.MONOSPACED,Font.PLAIN,9);


/**
 *	Font to be used in the popup-menu
 **/
Font	BUBBLE_MENU_FONT = BoardFont.getFont(Font.SANS_SERIF,Font.BOLD,9);


/**
 * Buda menu bar sizes
 */
int BUDA_MENU_BUTTON_ICON_WIDTH = 16;
int BUDA_MENU_BUTTON_ICON_HEIGHT = 16;

/**
 *	Color for the menu
 **/
String	BUDA_MENU_COLOR_PROP = "Buda.MenuColor";


/**
 *	Background color for the menu
 **/
String	BUDA_MENU_BACKGROUND_COLOR_PROP = "Buda.MenuBackgroundColor";


/**
 *	Color for the button panel
 **/
String	BUDA_BUTTON_PANEL_COLOR_PROP = "Buda.ButtonPanelColor";


/**
 * Size of inset around the Options button
 */
Insets BUDA_BUTTON_INSETS = new Insets(0,1,0,1);


/**
 * Vertical strut for spacing between buttons
 */
int BUDA_BUTTON_SEPARATION = 3;

/**
 * Width for resizing chevron icon for buttons
 */

int BUDA_BUTTON_RESIZE_WIDTH = 7;

/**
 * Height for resizing chevron icon for buttons
 */

int BUDA_BUTTON_RESIZE_HEIGHT = 4;


/**
 * Boolean to determine what the task shelf should be sorted by
 */
String TASK_SHELF_SORT_BY_DATE = "Buda.shelf.by.date";




/********************************************************************************/
/*										*/
/*	Layout Definitions							*/
/*										*/
/********************************************************************************/

/**
 *	Positioning information for a bubble.  Passed as part of a bubble constraint
 *	when defining the bubble.
 **/

enum BudaBubblePosition {
   NONE,			// unspecified
   MOVABLE,			// movable (default)
   FIXED,			// unmovable
   STATIC,			// floating on top
   FLOAT,			// fixed in viewport, floats with viewport
   DIALOG,			// floating, but on top of statics
   DOCKED,			// floating on top, docked to one side of the window
   USERPOS,			// temporarily floating to let user position
   HOVER			// fixed but on top of statics
}

/**
 *	Location Information for Docked Bubbles
 */
enum BudaBubbleDock {
	NORTH,
	SOUTH,
	WEST,
	EAST,
}



/********************************************************************************/
/*										*/
/*	Bubble placement options						*/
/*										*/
/********************************************************************************/

int	PLACEMENT_LEFT = 0x1;
int	PLACEMENT_RIGHT = 0x2;
int	PLACEMENT_BELOW = 0x4;
int	PLACEMENT_ABOVE = 0x8;
int	PLACEMENT_MOVETO = 0x10;	// force visible
int	PLACEMENT_GROUPED = 0x20;
int	PLACEMENT_LOGICAL = 0x40;

int	PLACEMENT_ADJACENT = 0x80;	// adjacent to window, not group
int	PLACEMENT_ADGROUP  = 0x100;	// adjacent to group, not window
int	PLACEMENT_USER = 0x200; 	// give user time to place the window
int	PLACEMENT_NEW = 0x400;		// mark as new window
int	PLACEMENT_ALIGN = 0x800;	// align with prior bubble rather than position
int	PLACEMENT_PREFER = 0x1000;	// use user preference for RIGHT/BELOW
int	PLACEMENT_EXPLICIT = 0x10000;	// no default placements



/********************************************************************************/
/*										*/
/*	Checkpoint constants							*/
/*										*/
/********************************************************************************/

/**
 *	Time between automatic checkpointing of editor buffers
 **/
int	BUBBLE_CHECKPOINT_TIME = 5*60*1000;		// 5 minutes



/********************************************************************************/
/*										*/
/*	Bubble Region Constants 						*/
/*										*/
/********************************************************************************/

/**
 *	This defines where the mouse is over a bubble area.
 **/

enum BudaRegion {
   NONE,
   BORDER_N,
   BORDER_NE,
   BORDER_E,
   BORDER_SE,
   BORDER_S,
   BORDER_SW,
   BORDER_W,
   BORDER_NW,
   COMPONENT,
   LINK,
   GROUP,
   GROUP_NAME;

   public boolean isBorder() {
      return (toString().startsWith("BORDER"));
    }
}




/********************************************************************************/
/*										*/
/*	Bubble Connection Constants						*/
/*										*/
/********************************************************************************/

/**
 *	This defines default port locations
 **/

enum BudaPortPosition {
   NONE,
   BORDER_N,
   BORDER_NE,
   BORDER_E,
   BORDER_SE,
   BORDER_S,
   BORDER_SW,
   BORDER_W,
   BORDER_NW,
   BORDER_EW,
   BORDER_EW_TOP,
   BORDER_NS,
   BORDER_ANY,
   CENTER
}



/**
 *	Interface provided for defining where links between bubbles start or end.
 *	The default implementation (BudaDefaultPort) provides for fixed connections,
 *	alternative implementations might provide other types of connections, e.g.
 *	one that is attached to a line number.
 *
 *	If getLinkPoint returns null then the link is not drawn.
 *
 **/

interface LinkPort {

/**
 *	Compute the link point for the source.	The tgt rectangle defines the
 *	position of the target bubble.
 **/
   Point getLinkPoint(BudaBubble b,Rectangle tgt);

/**
 *	Compute the link point for the target.	The tgt point defines the source
 *	of the link
 **/
   Point getLinkPoint(BudaBubble b,Point2D tgt);

/**
 *	Output the link so it can be reloaded
 **/
   void outputXml(BudaXmlWriter xw);

   void noteRemoved();

}


/**
 *	Width of a link between two bubbles.
 **/
float	LINK_WIDTH = 2.5f;			// width of a link line


/**
 *	Color of a normal link between bubbles.
 **/
String	LINK_COLOR_PROP = "Buda.LinkColor";


/**
 *	Color of a normal link between bubbles when the target has the focus (transitively).
 **/
String	LINK_FOCUS_COLOR_PROP = "Buda.LinkFocusColor";


/**
 *	Color for user-drawn links.
 **/
String	LINK_DRAW_COLOR_PROP = "Buda.LinkDrawColor";




/**
 *	This enumeration holds the different arc styles that are currently supported.
 **/

enum BudaLinkStyle {
   NONE,
   STYLE_SOLID,
   STYLE_DASHED,
   STYLE_REFERENCE,
   STYLE_FLIP_SOLID,
   STYLE_FLIP_DASHED,
   STYLE_FLIP_REFERENCE
}



/**
 *	This enumeration holds the different arc end styles that are currently supported.
 **/

enum BudaPortEndType {
   END_NONE,
   END_ARROW,
   END_CIRCLE,
   END_TRIANGLE,
   END_SQUARE,
   END_ARROW_FILLED,
   END_CIRCLE_FILLED,
   END_TRIANGLE_FILLED,
   END_SQUARE_FILLED
}


/**
 *	The size of the arrow or other end cap that is drawn for a bubble link.
 **/
double BUDA_LINK_END_SIZE = 3.0;





/********************************************************************************/
/*										*/
/*	Expose constants							*/
/*										*/
/********************************************************************************/

/**
 *	Milliseconds between frames in expose' animation
 **/
int	EXPOSE_ANIMATE_INTERVAL = 25;


/**
 *	Percent done of the animation in each frame.  This is a fraction so that 0.1 indicates
 *	10%.
 **/
double	EXPOSE_ANIMATE_DELTA = 0.10;




/********************************************************************************/
/*										*/
/*	Bubble Movement Directions						*/
/*										*/
/********************************************************************************/

/**
 *	Directions allowed for bubble movement.
 **/

enum BudaMovement {
   NONE,
   LEFT,
   RIGHT,
   LEFT_RIGHT,
   UP,
   UP_LEFT,
   UP_RIGHT,
   UP_LEFT_RIGHT,
   DOWN,
   DOWN_LEFT,
   DOWN_RIGHT,
   DOWN_LEFT_RIGHT,
   UP_DOWN,
   UP_DOWN_LEFT,
   UP_DOWN_RIGHT,
   ANY;

   public boolean okLeft() { return (ordinal() & 1) != 0; }
   public boolean okRight() { return (ordinal() & 2) != 0; }
   public boolean okUp() { return (ordinal() & 4) != 0; }
   public boolean okDown() { return (ordinal() & 8) != 0; }

}



/********************************************************************************/
/*										*/
/*	Bubble content types							*/
/*										*/
/********************************************************************************/

/**
 *	This is used to characterize the content name from a bubble.
 **/

enum BudaContentNameType {
   NONE,
   CLASS,
   METHOD,
   FIELD,
   CLASS_ITEM,		// all fields or class prefix or class initializers
   FILE,
   OVERVIEW
}



/********************************************************************************/
/*										*/
/*	Callbacks								*/
/*										*/
/********************************************************************************/

/**
 *	Callbacks from the BudaBubbleArea.  The method updateOverview is called
 *	whenever an overview of the area might need to be updated (e.g. when bubbles
 *	are created, destroyed, or resized).  The moveDelta method is called when
 *	the user has requested the viewport be modified by dragging the background.
 **/

interface BubbleAreaCallback extends EventListener {

   public void updateOverview();
   public void moveDelta(int dx,int dy);

}	// end of inner interface BubbleAreaCallback




/**
 *	Callbacks for monitoring the current set of bubbles.  The focusChanged
 *	callback is invoked whenever a bubble gains/loses the foucs.  The added
 *	and removed callbacks are called as bubbles are added and removed.  The
 *	action done callback is invoked after the user has moved or resized a
 *	bubble.
 **/

interface BubbleViewCallback extends EventListener {

   public void doneConfiguration();

   public void focusChanged(BudaBubble bb,boolean set);
   public void bubbleAdded(BudaBubble bb);
   public void bubbleRemoved(BudaBubble bb);
   public boolean bubbleActionDone(BudaBubble bb);

   public void workingSetAdded(BudaWorkingSet ws);
   public void workingSetRemoved(BudaWorkingSet ws);

   public void copyFromTo(BudaBubble from,BudaBubble to);

}



/********************************************************************************/
/*										*/
/*	Interfaces for specific bubbles 					*/
/*										*/
/********************************************************************************/

/**
 *	Interface that may be updated for a particular type of bubble to override
 *	the default way of outputing the bubble.  This lets different types of
 *	bubbles include particular information or eliminate a description of the
 *	internals if that is appropriate.
 **/

interface BudaBubbleOutputer {

   String getConfigurator();
   void outputXml(BudaXmlWriter xw);

}	// end of interface BubbleOutputer



/**
 *	A JComponent implementing this interface will not be encased in a bubble.
 **/

interface NoBubble { }



/**
 *	A Bubble implementing this will not use a freeze pane
 **/

interface NoFreeze { }


/********************************************************************************/
/*										*/
/*	Scaling definitions							*/
/*										*/
/********************************************************************************/

/**
 *	Component can implement this to tell bubbles to call it directly to
 *	handle scaling
 **/
interface Scalable {

   void setScaleFactor(double sf);

}


interface BudaBubbleScaler {

   Rectangle getScaledBounds(BudaBubble bb);

}




/********************************************************************************/
/*										*/
/*	Interface representing the search box					*/
/*										*/
/********************************************************************************/

enum SearchType {
   SEARCH_CODE,
   SEARCH_DOC,
   SEARCH_PEOPLE,
   SEARCH_EXPLORER,
   SEARCH_LAUNCH_CONFIG,
   SEARCH_ALL,
   SEARCH_COURSES
}


/**
 *	This represents whether a regular right click should pop up the SEARCH_ALL search box
 *	or the SEARCH_CODE search box
 */
String SEARCH_MERGED_ON_REGULAR = "Buda.mergeforreg";


/**
 *	Allow multiple search bubbles if true
 **/
String SEARCH_ALLOW_MULTIPLE = "Buda.search.multiple";




/**
 *	This interface is implemented by a routine that can create a search bubble
 *	for the various repositories.
 **/

interface SearchBoxCreator {
   BudaBubble createSearch(SearchType st,String project,String prefix);
   BudaBubble getPackageExplorer(BudaBubbleArea bba);
}


/**
 *	This interface is to be defined by a routine that can create a buda bubble
 *	for javadoc of the given name.
 **/

interface DocBoxCreator {
   BudaBubble createDocBubble(String fullname);
}


/********************************************************************************/
/*										*/
/*	Interfaces for reading in configurations				*/
/*										*/
/********************************************************************************/

/**
 *	This interface is used to create bubbles from the configuration file.  Bubble
 *	configurators are registered with Buda as part of initialization.  Each bubble
 *	in the configuration file has a type field which indicates what configurator
 *	should be used.  The configurator is then responsiple for creating the bubble.
 *
 *	Configurators are also responsible for generating the XML for a bubble and for
 *	storing and loading any data that should be saved in the project history file
 *	rather than the configuration file.
 **/

interface BubbleConfigurator {

   BudaBubble createBubble(BudaBubbleArea bba,Element xml);
   boolean matchBubble(BudaBubble bb,Element xml);
   void outputXml(BudaXmlWriter xw,boolean history);
   void loadXml(BudaBubbleArea bba,Element root);

}	// end of inner interface BubbleConfigurator



/**
 *	A port configurator is used by Buda to create specialized LinkPorts that
 *	may be defined in other modules.  There job is to recreate the port from
 *	the XML that is written by the port itself as part of the configuration file.
 **/

interface PortConfigurator {

   LinkPort createPort(BudaBubble bb,Element xml);

}	// end of inner interface PortConfigurator




/********************************************************************************/
/*										*/
/*	Interface for Buda menu button handling 				*/
/*										*/
/********************************************************************************/

/**
 *	This interface is used as the callback for routines that want to register
 *	a button that is associated with the search box.
 **/

interface ButtonListener extends EventListener {

   public void buttonActivated(BudaBubbleArea bba,String id,Point pt);

}



/********************************************************************************/
/*										*/
/*	Interface for drag and drop bubbles					*/
/*										*/
/********************************************************************************/

/**
 *	This interface is used by clients that provide a means of dragging and then
 *	dropping one or more bubbles.  It represents that data to be transferred.
 **/

interface BudaDragBubble {

   BudaBubble [] createBubbles();

}


/********************************************************************************/
/*										*/
/*	Interface for bubble content location					*/
/*										*/
/********************************************************************************/

/**
 *	This interface represents the contents of a bubble.  It is implemented by
 *	BudaBubble, but could be implemented by non-bubbles that need to provide
 *	content-based awareness.
 **/

interface BudaContentLocation {

   String getContentProject();
   File getContentFile();
   String getContentName();
   BudaContentNameType getContentType();

}



/********************************************************************************/
/*										*/
/*	Interface for bubble creators						*/
/*										*/
/********************************************************************************/

/**
 *	NOT IMPLEMENTED OR USED YET.
 **/

interface BudaBubbler {

   BudaBubble createBubble(BudaBubble source,String target,BudaContentLocation loc,Point pos);

}




/********************************************************************************/
/*										*/
/*	Interface for hint bubbles						*/
/*										*/
/********************************************************************************/

/**
 *	This interface defines the callbacks that can be used in defining a HINT-like
 *	bubble that will fade over time.  An example are the bubbles that let you undo
 *	the deletion of a bubble or a bubble group.
 **/

interface BudaHintActions {

   void clickAction();
   void finalAction();

}	// end of inner interface BudaHintActions



/********************************************************************************/
/*										*/
/*	Interface for handling save, checkpoint and similar operations		*/
/*										*/
/********************************************************************************/

/**
 *	This interface provides hooks that are invoked on files.  Save requests
 *	are invoked on all registered handlers when the user does a Save All.
 *	Checkpoint requests are invoked periodically.  Quit callbacks are invoked
 *	on exit.
 *
 *	All instance of BudaBubble implement this interface (generally doing
 *	nothing).
 **/

interface BudaFileHandler extends EventListener {

/**
 *	Invoked when the user does a SaveAll at the top level.
 **/
   void handleSaveRequest();

   void handleSaveDone();

/**
 *	Invoked when the user does a CommitAll at the top level.
 **/
   void handleCommitRequest();

/**
 *	Invoked periodically to ensure work is not lost.
 **/
   void handleCheckpointRequest();


/**
 *	Invoked when the user attempts to exit.  If this returns false then the
 *	exit attempt will be aborted.
 **/
   boolean handleQuitRequest();

/**
 *	Invoked when properties have changed.
 **/
   void handlePropertyChange();

}	// end of interface BudaFileHandler




/********************************************************************************/
/*										*/
/*	Interface for working sets						*/
/*										*/
/********************************************************************************/

enum WorkingSetGrowth {
   NONE, GROW, EXPAND
}

interface BudaWorkingSet {

/**
 *	Get a temp file describing the working set.
 **/
   File getDescription() throws IOException;

/**
 *	Return the region defined by the working set
 **/
   Rectangle getRegion();

/**
 *	Return the bubble area associated with the working set
 **/
   BudaBubbleArea getBubbleArea();

/**
 *	Return the current label (name) for the working set
 **/
   String getLabel();

/**
 *	Change the label associated with the working set
 **/

   void setLabel(String s);

   boolean isSticky();

   Object getProperty(String prop);

   void setProperty(String prop,Object v);

}	// end of interface BudaWorkingSet



/********************************************************************************/
/*										*/
/*	Interface for sharing							*/
/*										*/
/********************************************************************************/

interface BudaShare extends Comparable<BudaShare> {

   String getId();
   String getUser();
   String getName();
   String getHost();

}	// end of interface BudaShare



/********************************************************************************/
/*										*/
/*	FocusOnEntry: add as a mouse listener to grab focus on mouse entry	*/
/*										*/
/********************************************************************************/

/**
 *	This is a helper class that classes might add as a mouse listener
 *	to effect focus on entry.
 **/

class FocusOnEntry extends MouseAdapter implements Serializable {

   private JComponent for_component;

   private static final long serialVersionUID = 1;

   public FocusOnEntry()			{ for_component = null; }
   public FocusOnEntry(JComponent c)		{ for_component = c; }

   @Override public void mouseEntered(MouseEvent e) {
      JComponent c = (for_component == null ? (JComponent) e.getSource() : for_component);
      c.requestFocusInWindow();
    }

}	// end of inner class FocusOnEntry



/********************************************************************************/
/*										*/
/*	Help Client Interface							*/
/*										*/
/********************************************************************************/

interface BudaHelpClient {

   String getHelpText(MouseEvent e);
   String getHelpLabel(MouseEvent e);

}	// end of inner interface BudaHelpClient


interface BudaDemonstration {

   void stopDemonstration();

}	// end of inner interface BudaDemonstration


interface BudaHelpRegion {

   BudaRegion getRegion();
   BudaBubble getBubble();
   BudaBubbleGroup getGroup();
   BudaBubbleLink getLink();

}	// end of interface BudaHelpRegion



}	// end of interface BudaConstants




/* end of BudaConstants.java */
