/********************************************************************************/
/*										*/
/*		BaleConstants.java						*/
/*										*/
/*	Bubble Annotated Language Editor constant definitions			*/
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


package edu.brown.cs.bubbles.bale;

import edu.brown.cs.bubbles.board.BoardConstants;
import edu.brown.cs.bubbles.board.BoardProperties;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.bump.BumpConstants;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JPopupMenu;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.Keymap;
import javax.swing.text.Position;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Collection;
import java.util.EventListener;
import java.util.List;



/**
 *	Definitions for the BALE code fragment editing package.
 **/

public interface BaleConstants extends BumpConstants, BoardConstants {



/********************************************************************************/
/*										*/
/*	Properties location							*/
/*										*/
/********************************************************************************/

static BoardProperties	 BALE_PROPERTIES = BoardProperties.getProperties("Bale");



/********************************************************************************/
/*										*/
/*	Fragment Editor Types							*/
/*										*/
/********************************************************************************/

/**
 *	The different types of code fragments that we support.
 **/

enum BaleFragmentType {
   NONE,
   METHOD,
   CLASS,
   FILE,
   FIELDS,				// set of fields (python attributes)
   STATICS,				// static initializers (python evaluations)
   MAIN,				// python main program
   HEADER,				// class header
   IMPORTS,				// set of import statements
   EXPORTS,				// set of export statements
   CODE,				// set of evaluations
   ROFILE,				// Read-only file (not in IDE)
   ROMETHOD,				// read-only method (not in IDE)
}



/********************************************************************************/
/*										*/
/*	Token definitions							*/
/*										*/
/********************************************************************************/

/**
 *	The initial state for tokenization.  This represents the state at the
 *	beginning of a line.  The state captures the fact that a block comment
 *	might have been started on a prior line and not yet ended.
 **/

enum BaleTokenState {
   NORMAL,
   IN_FORMAL_COMMENT,
   IN_COMMENT,
   IN_LINE_COMMENT, //amc6 added
   IN_STRING,
   IN_MULTILINE_STRING,
}



/**
 *	The various types of tokens that we parse things into.	Note that this set
 *	is not comprehensive.  Several keywords get mapped into KEYWORD and many
 *	operators get mapped to OP.  The selection of tokens is based on what is
 *	needed both indentation computation and for determining how to format
 *	different elements in the editor.
 **/

enum BaleTokenType {
   NONE,				// undefined
   EOF, 				// end of file
   EOL, 				// end of line
   SPACE,				// white space (no end of line)
   LINECOMMENT, 			// single line comment with possible eol
   EOLFORMALCOMMENT,			// middle of formal comment with eol
   ENDFORMALCOMMENT,			// end of formal comment
   EOLCOMMENT,				// middle of comment with eol
   ENDCOMMENT,				// end of comment
   KEYWORD,				// keyword other those below
   RETURN,				// the keyword return
   IF,					// the keyword if
   DO,					// the keyword do
   FOR, 				// the keyword for
   TRY, 				// the keyword try
   NEW, 				// the keyword new
   CASE,				// the keyword case
   ELSE,				// the keyword else
   ENUM,				// the keyword enum
   GOTO,				// the keyword goto
   BREAK,				// the keyword break
   CATCH,				// the keyword catch
   CLASS,				// the keyword class
   WHILE,				// the keyword while
   STATIC,				// the keyword static
   SWITCH,				// the keyword switch
   DEFAULT,				// the keyword default
   FINALLY,				// the keyword finally
   INTERFACE,				// the keyword interface
   SYNCHRONIZED,			// the keyword synchronized
   CONTINUE,				// the keyword continue
   PASS,				// the keyword pass
   RAISE,				// the keyword raise
   IMPORT,				// the keyword import
   PACKAGE,				// the keyword package
   TYPEKEY,				// type keyword (int, void, ...)
   NUMBER,				// numeric literal
   CHARLITERAL, 			// character literal
   STRING,				// string literal
   LONGSTRING,				// long string literal (multiline)
   IDENTIFIER,				// identifier
   LPAREN,				// left paren (
   RPAREN,				// right paren )
   LBRACE,				// left brace {
   RBRACE,				// right brace }
   LBRACKET,				// left bracket [
   RBRACKET,				// right bracket ]
   SEMICOLON,				// semicolon ;
   COMMA,				// comma ,
   COLON,				// colon :
   QUESTIONMARK,			// question mark ?
   LANGLE,				// left angle bracket <
   RANGLE,				// right angle bracket >
   DOT, 				// period .
   AT,					// at sign @
   OP,					// valid operator
   EQUAL,				// equals sign
   BACKSLASH,				// backslash
   BADSTRING,				// unclosed string
   BADCHARLIT,				// unclosed character literal
   BADNUMBER,				// 0x, ###e ###e+ ###e-
   ELIDED,				// elision image
   OTHER				// illegal token or character
}


/**
 *	Interface representing a token from tokenization
 **/

interface BaleToken {

/**
 *	The type of the token
 **/
   BaleTokenType getType();


/**
 *	Where the token begins
 **/
   int getStartOffset();


/**
 *	How long the token is
 **/
   int getLength();
}


int EXDENT_UNKNOWN = -10;
int EXDENT_CONTINUE = -11;



/********************************************************************************/
/*										*/
/*	AST node types								*/
/*										*/
/********************************************************************************/

/**
 *	The different types of AST nodes that we are interested in.  Note that this
 *	is not a comprehensive list but rather denotes just those nodes that are
 *	needed for determining the type of elements to be created and hence how to
 *	best do elision and reflow.
 **/

enum BaleAstNodeType {
   NONE,
   SET, 			// set of elements
   BLOCK,
   SWITCH_BLOCK,
   STATEMENT,
   EXPRESSION,
   ANNOTATION,
   CLASS,
   FIELD,
   INITIALIZER,
   METHOD,
   FILE,
   IMPORT
}



/**
 *	The different type of identifiers we are interested in.  These are deduced
 *	from the elision information returned from the IDE.  The set of elements here
 *	is not meant to be comprehensive, but does represent what is needed to distinguish
 *	between different elements, generally for the purpose of formatting in the
 *	editor.
 **/

enum BaleAstIdType {
   NONE,
   CALL,
   CALL_STATIC,
   CALL_DEPRECATED,
   CALL_UNDEF,
   TYPE,
   FIELD,
   FIELD_STATIC,		// static field
   FIELDC,			// field constant (final)
   ENUMC,			// enum constant
   CLASS_DECL,
   CLASS_DECL_MEMBER,
   METHOD_DECL,
   EXCEPTION_DECL,
   FIELD_DECL,
   LOCAL_DECL,
   UNDEF,
   ANNOT,
   BUILTIN,
   MODULE
}



/********************************************************************************/
/*										*/
/*	Elision modes								*/
/*										*/
/********************************************************************************/

/**
 *	These constants indicate how elision is to be done within the system.
 *	If the current mode is ELIDE_CHECK_ALWAYS, then elision is done continually
 *	as the cursor moves to ensure that the current location is emphasized.
 *	ELIDE_CHECK_NEVER indicates that the current elision choices should be kept.
 *	ELIDE_CHECK_ONCE causes elision to be computed the next time and then the
 *	mode reset to ELIDE_CHECK_NEVER.  ELIDE_NONE indicates that no elision should
 *	be done.
 **/

enum BaleElideMode {
   ELIDE_CHECK_NEVER,			// leave as is
   ELIDE_CHECK_ONCE,			// check once, then leave as is
   ELIDE_CHECK_ALWAYS,			// redo elision as the cursor moves
   ELIDE_NONE				// no elision
}


interface BaleElisionData {
   int getStartOffset();
   int getEndOffset();
   String getElementName();
}	// end of inner class BaleElisionData



enum BaleSplitMode {
   SPLIT_NEVER, 			// don't split lines
   SPLIT_QUICK, 			// fast check
   SPLIT_NORMAL 			// normal (best) check
}

int	SPLIT_QUICK_SIZE = 10240;



/********************************************************************************/
/*										*/
/*	Element View types							*/
/*										*/
/********************************************************************************/

/**
 *	Characterizations of the different types of structure elements comprising
 *	the code fragment.  BLOCKS are outside of a line, typically containing multiple
 *	lines; LINE is the element holding a single line; CODE is a block element inside
 *	of a line (e.g. an expression); TEXT is a primitive element.
 **/

enum BaleViewType {
   NONE,		// ignore
   BLOCK,		// outside line
   LINE,		// line element
   CODE,		// inside line element
   TEXT,		// use a BaleViewText
   ORPHAN		// orphan display
}



/********************************************************************************/
/*										*/
/* Types for Information bubbles						*/
/*										*/
/********************************************************************************/

enum BaleInfoBubbleType {
   IMPLDOC,
   DEFDOC,
   DOC,
   REF,
   NOIDENTIFIER,
   UNDEFINED,
}


enum BaleInfoBubbleIconType {
   WARNING,
   ERROR,
}



/********************************************************************************/
/*										*/
/*	Highlighting types							*/
/*										*/
/********************************************************************************/

/**
 *	The different types of token-based highilighting that are supported.  These
 *	should probably be defined extensibly in a resource file rather than being
 *	hard-coded.
 **/

enum BaleHighlightType {
   NONE,
   IDENTIFIER,
   IDENTIFIER_WRITE,
   IDENTIFIER_DEFINE,
   BRACKET,
   FIND
}


enum BaleStackType {
   NORMAL,                      // all bubbles, one bubble -> no stack             
   DROP_SOURCE,                 // ignore an instance from the source
   FORCE,                       // force a stack even with one bubble
}



/**
 *	The color for normal identifier highlighing.
 **/
String BALE_IDENTIFIER_HIGHLIGHT_COLOR_PROP = "Bale.identifier.highlight.color";

/**
 *	The color for highlighting a write instance of an identifier.
 **/
String BALE_IDENTIFIER_WRITE_HIGHLIGHT_COLOR_PROP = "Bale.identifier.write.highlight.color";


/**
 *	The color for highlighting the defining instance of an identifier.
 **/
String BALE_IDENTIFIER_DEFINE_HIGHLIGHT_COLOR_PROP = "Bale.identifier.define.highlight.color";



/**
 *	The color for highlighting the matching bracket.  Note that this is
 *	not implemented yet.
 **/
String BALE_BRACKET_HIGHLIGHT_COLOR_PROP = "Bale.bracket.highlight.color";



/**
 *	The color for highlighting the results of a find.
 **/
String BALE_FIND_HIGHLIGHT_COLOR_PROP = "Bale.FindHighlightColor";



/**
 *	Color for port annotations
 **/
String BALE_PORT_ANNOT_COLOR_PROP = "Bale.PortAnnotColor";




/********************************************************************************/
/*										*/
/*	Colors and fonts							*/
/*										*/
/********************************************************************************/

/**
 *	The color for the underline for highlighting errors in the text.
 **/
String BALE_ERROR_COLOR_PROP = "Bale.error.color";

/**
 *	The color for underling for highlighting warnings in the text.
 **/
String BALE_WARNING_COLOR_PROP = "Bale.warning.color";

/**
 *	The color for the annotation area.
 **/
String BALE_ANNOT_BAR_COLOR_PROP = "Bale.AnnotBarColor";
String BALE_ANNOT_LINE_NUMBER_COLOR = "Bale.LineNumberColor";

/**
 *	The color for the caret.
 **/
String BALE_CARET_COLOR_PROP = "Bale.CaretColor";


/**
 *	The background color for the editor when there are errors in the code.
 **/
String BALE_ERROR_BACKGROUND_PROP = "Bale.error.background";
String BALE_ERROR_USE_BKG = "Bale.error.use.background";


/**
 *	The normal top color of the editor.
 **/
String BALE_EDITOR_TOP_COLOR_PROP = "Bale.editor.top.color";


/**
 *	The normal bottom color of the editor.
 **/
String BALE_EDITOR_BOTTOM_COLOR_PROP = "Bale.editor.bottom.color";


/**
 *  Boolean: should we do gradient?
 */
String BALE_EDITOR_DO_GRADIENT = "Bale.editor.do.gradient";


/**
 *  Boolean: should we do elision?
 */
String BALE_EDITOR_NO_ELISION = "Bale.editor.no.elision";
String BALE_EDITOR_ALWAYS_ELIDE = "Bale.editor.always.elide";


/**
 * Boolean: should we do reflow
 **/
String BALE_EDITOR_NO_REFLOW = "Bale.editor.no.reflow";


/**
 *  Boolean: should we do preview when hovering?
 **/
String BALE_PREVIEW_ENABLE = "Bale.preview.enable";


/**
 *	The color to use for the line indicating that budding is possible.
 **/
String BALE_BUD_LINE_COLOR_PROP = "Bale.BudLineColor";


/**
 *	The line format for the line indicating that budding is possible.
 **/
Stroke BALE_BUD_LINE_STROKE = new BasicStroke(1f,BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER,1f,
						 new float [] { 4f,2f }, 0f);





/**
 *	The font to be used in the crumb bar
 **/
String BALE_CRUMB_FONT = "Bale.crumb.font";



/**
 *  Boolean which determines whether the crumb bar should show method names
 */
String BALE_CRUMB_SHOW_METHOD = "Bale.crumb.showmethod";

/**
 *	The font used for methods in the crumb bar
 **/
String BALE_CRUMB_METHOD_FONT = "Bale.crumb.font";



/**
 *	The color for component (package elements) text in the crumb bar.
 **/
String BALE_CRUMB_COMPONENT_COLOR_PROP = "Bale.CrumbComponentColor";


/**
 *	The color for the package text in the crumb bar.
 **/
String BALE_CRUMB_PACKAGE_COLOR_PROP = "Bale.CrumbPackageColor";


/**
 *	The color for class text in the crumb bar.
 **/
String BALE_CRUMB_CLASS_COLOR_PROP = "Bale.CrumbClassColor";

/**
 *	The color for method text in the crumb bar. Not in use
 **/
String BALE_CRUMB_METHOD_COLOR_PROP = "Bale.CrumbMethodColor";



/**
 *	The background color when the mouse rolls over an area of the crumb bar.
 **/
String BALE_CRUMB_ROLLOVER_COLOR_PROP = "Bale.CrumbRolloverColor";


/**
 *	Help icon for the crumb bar
 **/
String BALE_CRUMB_HELP_ICON = "crumbhelp.png";




/********************************************************************************/
/*										*/
/*	Caret Definitions							*/
/*										*/
/********************************************************************************/

/**
 *	The different styles of caret that are supported by the editor. Note that
 *	not all of these are used currently
 **/

enum BaleCaretStyle {
   BLOCK_CARET,
   LINE_CARET,
   THIN_LINE_CARET,
   THICK_LINE_CARET
}

/**
 *	The width in pixels of the LINE_CARET.
 **/
int BALE_LINE_CARET_WIDTH = 2;			// width of text editing caret


/**
 *	The width in pixels of the THIN_LINE_CARET.
 **/
int BALE_THIN_CARET_WIDTH = 1;			// width of thin text editing caret


/**
 *	The width in pixels of the THICK_LINE_CARET.
 **/
int BALE_THICK_CARET_WIDTH = 3; 		// width of thick text editing caret

/**
 *	Boolean to determine the style of caret movement
 */
String BALE_DOES_DOCUMENT_MOVEMENT = "Bale.caret.movement";




/********************************************************************************/
/*										*/
/*	Editor size constants for reflow and elision				*/
/*										*/
/********************************************************************************/

/**
 *  Whether comments wrap or not.
 */
String COMMENT_WRAPPING = "Bale.comment.wrapping"; //added by amc6

/**
 *  Whether strings wrap or not.
 */
String STRING_WRAPPING = "Bale.string.wrapping"; //added by amc6

/**
 *	The minimum initial width for a code bubble.
 **/
int	BALE_MIN_WIDTH = 300;			// minimum size for a bale bubble


/**
 *	The minimum initial height for a code bubble.
 **/
int	BALE_MIN_HEIGHT = 100;


/**
 *	The maximum height to which a code bubble will automatically grow.
 **/
int	BALE_MAX_GROW_HEIGHT = 500;		// max size for bubble growing



/**
 *	Initial width to add to preferred bubble size.
 **/
int	BALE_DELTA_INITIAL_WIDTH = 64;


/**
 *	Extra width to add to a bubble to allow easier editing
 **/
int	BALE_DELTA_EDITING_WIDTH = 48;


/**
 *	Initial height to add to preferred bubble size.
 **/
int	BALE_DELTA_INITIAL_HEIGHT = 15;


/**
 *	Maximum initial width for a code bubble.
 **/
int	BALE_MAX_INITIAL_WIDTH = 500;		// max initial size for a bubble


/**
 *	Maximum initial height for a code bubble editor area
 **/
int	BALE_MAX_INITIAL_HEIGHT = 400;


/**
 *	Maximum initial height for a code bubble.
 **/
int	BALE_MAX_INITIAL_BUBBLE_HEIGHT = 400;


/**
 *	Height for drawing an ellipses when eliding code.
 **/
int	BALE_ELLIPSES_HEIGHT = 10;		// height for ellipses drawing


/**
 *	Delta left/right for click on ellipses being expand vs position
 **/
int	BALE_ELLIPSES_INSIDE_DELTA = 5; 	// width for click inside ellipses


/**
 *	Height for a shrunk (elided) blank line.
 **/
int	BALE_EMPTY_HEIGHT = 2;			// height for shrunk blank line


/**
 *	Height for an elided comment.  This represents the comment ellipses.
 **/
int	BALE_COMMENT_HEIGHT = 10;		// height for shrunk block comment



/**
 *	Maximum white space to allow when doing reflow on secondary lines
 **/
int	BALE_MAX_REFLOW_INDENT = 9;	       // maximum reflow indentation


/**
 *	Maximum initial white space to allow on a line that has to be reflowed.
 **/
int	BALE_MAX_INITIAL_REFLOW_INDENT = 9;


/**
 *	Priority scale factor for comments to encourage their elision.
 **/
double	BALE_COMMENT_PRIORITY = 0.10;


/**
 *	Multiplier for elision priority based on distance away from the current cursor.
 *	This applied multiplicatively, i.e. once for each step the current item is away
 *	from the current position.  This constants affects moving up in the syntax tree.
 **/
double	BALE_CARET_UP_PRIORITY = 0.9;


/**
 *	Multiplier for elision priority based on distance away from the current cursor.
 *	This applied multiplicatively, i.e. once for each step the current item is away
 *	from the current position.  This constants affects moving down in the syntax tree.
 **/
double	BALE_CARET_DOWN_PRIORITY = 0.9;



/**
 *	Initial minimum height of a Bale-based bubble stack.
 **/
int	BALE_STACK_INITIAL_HEIGHT = 200;


/**
 *	Initial minimum width of a Bale-based bubble stack.
 **/
int	BALE_STACK_INITIAL_WIDTH = 300;


/**
 *	Width of the annotation area.
 **/
int	BALE_ANNOT_WIDTH = 12;
int	BALE_ANNOT_NUMBER_WIDTH = 30;
String BALE_ANNOT_FONT = "Bale.annot.font";


/**
 *	Percentage of editor size before adding scroll bars
 **/
int	BALE_SCROLL_MINIMUM = 150;


/**
 *	Minimum height for scroll bars
 **/
int	BALE_SCROLL_MIN_HT = 300;





/********************************************************************************/
/*										*/
/*	Indentation constants							*/
/*										*/
/********************************************************************************/

String BALE_DO_INDENT = "Bale.do.indent";



/********************************************************************************/
/*										*/
/*	Backup and checkpoint definitions					*/
/*										*/
/********************************************************************************/

/**
 *	The directory name to be used for checkpointing open and changed files.
 **/
String BALE_CHECKPOINT_DIRECTORY = "bBACKUP";


/**
 *	The file extension to be used when checkpointing open and changed files.
 **/
String BALE_CHECKPOINT_EXTENSION = ".ckpt";





/********************************************************************************/
/*										*/
/*	Timing/Delay definitions						*/
/*										*/
/********************************************************************************/

String BALE_TYPEIN_DELAY = "Bale.edide.delay";
String BALE_HIGHLIGHT_DELAY = "Bale.highlight.delay";
String BALE_AUTOCOMPLETE_DELAY = "Bale.autocomplete.delay";



/********************************************************************************/
/*										*/
/*	Class for saved positions						*/
/*										*/
/********************************************************************************/

/**
 *	This interface represents a set of positions that can be saved and
 *	restored that do their best to keep their associated lines even as
 *	the underlying file changes.
 *
 *	This can be used (when fully implemented -- there is no load/save at
 *	this point) to allow notes and other annotations to be attached to
 *	source files with the attachment points maintained over time.  It can
 *	also be used to accurately adjust breakpoints and the like.
 **/

interface BaleSavedPositions { }




/********************************************************************************/
/*										*/
/*	Class for fragments							*/
/*										*/
/********************************************************************************/

/**
 *	This interface represents the access needed to a fragment independent
 *	of its incorporation into an editor or document.
 **/

interface BaleFragment	{

/**
 *	This is called when a file needs to be reloaded.  It saves whatever needs
 *	to be preserved (i.e. annotations, positions), and then restores them when
 *	the reload is complete.
 **/
   BaleReloadData startReload();


/**
 *	This is called when the abstract syntax tree for the fragment has been
 *	updated by the underlying IDE.	It typically will cause the editor to
 *	reformat the text.
 **/
   void handleAstUpdated(List<BaleAstNode> rootnodes);


/**
 *	This is called when the set of problems associated with the fragment have
 *	been updated (i.e. new problems added, problems removed, problems changed).  It
 *	can updated the display accordingly.
 **/
   void handleProblemsUpdated();

}	// end of inner interface BaleFragment




/**
 *	This interface represents data that was saved before a file was reloaded after
 *	being changed externally (or reverted).
 **/

interface BaleReloadData {

/**
 *	Restore the previously saved data for the new version of the file.
 **/
   void finishedReload();

}	// end of inner interface BaleReloadData



/**
 *	This interface allows editors to be notified of annotation events
 **/

interface BaleAnnotationListener extends EventListener {

/**
 *	Note that an annotation has been added
 **/
   void annotationAdded(BaleAnnotation ba);

/**
 *	Note that an annotation has been removed
 **/
   void annotationRemoved(BaleAnnotation ba);

}	// end of inner interface BaleAnnotationListener



/********************************************************************************/
/*										*/
/*	Interface for editors							*/
/*										*/
/********************************************************************************/

/**
 *	This interface represents an abstract representation of what the rest of
 *	Bale package needs from an editor.  It is a subset of the methods of a
 *	JEditorPane combined with the methods specific to Bale.
 **/

interface BaleEditor {

/**
 *	Return the corresponding document.
 **/
   BaleDocument getBaleDocument();





/**
 *	Increase the size of the editor bubble by nline lines if possible.
 **/
   void increaseSize(int nline);

/**
 *	Flip the mode between overwrite and insert.
 **/
   void setOverwriteMode(boolean fg);

/**
 *	Return whether the current editor mode is overwrite or insert
 **/
   boolean getOverwriteMode();

/**
 *	Return the find bar widget associated with the editor
 **/
   BaleFinder getFindBar();

/**
 *	Return the annotation area widget associated with the editor
 **/
   BaleAnnotationArea getAnnotationArea();

/**
 *	Set the autocompletion context for the editor.	Setting it to null removes
 *	any completion attempts.
 **/
   void setCompletionContext(BaleCompletionContext c);

/**
 *	Return the current autocompletion context if any.
 **/
   BaleCompletionContext getCompletionContext();


/**
 *	Set the rename context for the editor.	Setting it to null removes
 *	any rename attempt.
 **/
   void setRenameContext(BaleRenameContext c);

/**
 *	Return the current renaming context if any.
 **/
   BaleRenameContext getRenameContext();

}	// end of interface BaleEditor




/**
 *	This interface represents an annotation. This can be a breakpoint, an error or
 *	warning or notice indicator, a note attachement, etc.  Annotations are associated
 *	with a file position but are typically displayed on a line basis, either using
 *	the annotation area with an appropriate icon or by highlighting the whole line
 *	using a colored background.
 **/

interface BaleAnnotation {



/**
 *	Return the file for this annotation
 **/
   File getFile();

/**
 *	Return the editor offset associated with the annotation.  This is a file offset,
 *	not a fragment offset.	It is in java terms, not eclipse terms.
 **/
   int getDocumentOffset();

/**
 *	Return the icon for the annotation area associated with this annotation. This
 *	returns null if there is no associated icon (e.g. line highlighting only).
 **/
   Icon getIcon(BudaBubble bbl);

/**
 *	Return the tool tip associated with this annotation.
 **/
   String getToolTip();

/**
 *	Return the background color for highlighting the line associated with this
 *	annotation.  This returns null if there is no line highlighting (e.g. icon only).
 **/
   Color getLineColor(BudaBubble bbl);

/**
 *	Return the background annotation area color or null if none.
 **/
   Color getBackgroundColor();


/**
 *	Check if adding this annotation should ensure that the given location is
 *	not elided and visible
 **/
   boolean getForceVisible(BudaBubble bbl);

/**
 *	Return the priority for the annotation.  Annotations of higher priority are
 *	drawn on top of annotations of lower priority.	Default priority is 10
 **/
   int getPriority();

/**
 *	Add buttons to the context menu for this annotation
 **/
   void addPopupButtons(Component base,JPopupMenu menu);

}	// end of interface BaleAnnotation



/**
 *	This interface represents a generic View.  Views are supported by Documents and
 *	represents structured components of the display.  The methods here are used to
 *	do reflow and elision.
 **/

interface BaleView {

/**
 *	Return the height of this construct at the given priority and the given width.	The
 *	priority is used to determine whether to elide an element or not (elements with a
 *	priority below p are elided; those above are not).  The width is used to determine
 *	whether reflow is necessary which in turn changes the height.
 **/
   float getHeightAtPriority(double p,float w);

/**
 *	Return the desired width at the given priority.  This routine can be used to support
 *	elision within a line (e.g. eliding an expression but maintaining the overall control
 *	constructs).  Right now this is not done.
 **/
   float getWidthAtPriority(double p);

}	// end of interface BaleView



/********************************************************************************/
/*										*/
/*	Interfaces for overivews						*/
/*										*/
/********************************************************************************/

/**
 *	This interface represents an extension of a Swing Document to include calls
 *	to map offsets between the IDE and ourselves (Java) and to track positions by
 *	line number.
 *
 *	Offsets may differ between the IDE and Java because of the treatment of new lines.
 *	Java always uses a '\n' for a new line.  The IDE typically will use whatever the
 *	user used or the underlying system convention (i.e. \n or \r\n).  We provide routines
 *	that map offsets for a file from one system to another.  Note that this same
 *	mechanism could be used for tabs and other special characters if needed.
 *
 *	Swing treats files in terms of absolute position.  Within the IDE there is need to
 *	look at things in terms of lines (e.g. breakpoints, errors, line-oriented editing
 *	commands).  The other functions provide mappings between file positions and line
 *	numbers.
 *
 *	This interface is also the view that is given to outside packages that might
 *	want to manipulate files directly without going through the user.  The document
 *	interface here reflects changes both to all on-screen bubbles and to the back
 *	end IDE.
 **/

interface BaleFileOverview extends Document {

/**
 *	Return the file associated with this editor
 **/
   File getFile();

/**
 *	Convert a java (internal) file position to an IDE (Eclipse) file position.
 **/
   int mapOffsetToEclipse(int off);

/**
 *	Convert an IDE (Eclipse) file position to a Java (internal) file position.
 **/
   int mapOffsetToJava(int off);

/**
 *	Find the file position corresponding to the start of the given line number.
 **/
   int findLineOffset(int line);

/**
 *
 **/
   int getFragmentOffset(int off);

/**
 *	Find the line number for the line that contains the given file position.
 **/
   int findLineNumber(int off);

/**
 *	Ensure position is maintained over saves
 **/
   Position savePosition(Position p);

/**
 *	Handle text editing
 **/
   boolean replace(int off,int len,String text,boolean format,boolean indent);

}	// end of interface BaleFileOverview




/********************************************************************************/
/*										*/
/*	Visualization Constants 						*/
/*										*/
/********************************************************************************/

/**
 * Enable visualization icons
 **/
String VISUALIZATION_ICON_ENABLE =  "visualization.icon.enable";

/**
 * Indication of visualization icons
 **/
String VISUALIZATION_ICON_INDICATION =	"visualization.icon.indication";

/**
 * Transparency of visualization icons
 **/
String VISUALIZATION_ICON_TRANSPARENT =  "visualization.icon.transparent";

/**
  * Size of visualization icons
 **/
String VISUALIZATION_ICON_SIZE =  "visualization.icon.size";

/**
 * Location of visualization icons
 **/
   String VISUALIZATION_ICON_LOCATION =  "visualization.icon.location";

/**
 * Enable visualization icons
 **/
String VISUALIZATION_GRADIENT_ENABLE =	"visualization.gradient.enable";

/**
  * Indication of visualization gradient
 **/
String VISUALIZATION_GRADIENT_INDICATION = "visualization.gradient.indication";

/**
  * Direction of gradient
 **/
String VISUALIZATION_GRADIENT_DIRECTION = "visualization.gradient.direction";


/**
 * Colors of gradient background
**/
String VISUALIZATION_GRADIENT_COLORS = "Bale.visualization.gradient.color.";

/**
 * Number of visualization icons
 **/
int NUM_ICONS = 11;



/********************************************************************************/
/*										*/
/*	Definitions for active regions						*/
/*										*/
/********************************************************************************/

/**
 *	Action associated with mouse click on a region
 **/

interface RegionAction {

/**
 *	Handle a mouse click on the region.
 **/

   void handleClick(MouseEvent e);
   BudaBubble handleHoverBubble(MouseEvent e);

}


/********************************************************************************/
/*										*/
/*	Definitions for external hover and popup management			*/
/*										*/
/********************************************************************************/

enum BaleContextType {
   NONE,
   FIELD_ID,
   LOCAL_ID,
   STATIC_FIELD_ID,
   CLASS_DECL_ID,
   METHOD_DECL_ID,
   LOCAL_DECL_ID,
   FIELD_DECL_ID,
   CALL_ID,
   STATIC_CALL_ID,
   UNDEF_CALL_ID,
   ANNOTATION_ID,
   UNDEF_ID,
   TYPE_ID,
   CONST_ID
}


interface BaleContextConfig {

   BudaBubble getEditor();
   BaleFileOverview getDocument();
   int getOffset();
   int getDocumentOffset();
   String getToken();
   BaleContextType getTokenType();
   String getMethodName();
   boolean inAnnotationArea();
   int getLineNumber();
   int getSelectionStart();
   int getSelectionEnd();

}	// end of interface BaleContextConfig



interface BaleContextListener extends EventListener {

   BudaBubble getHoverBubble(BaleContextConfig cfg);

   void addPopupMenuItems(BaleContextConfig cfg,JPopupMenu menu);

   String getToolTipHtml(BaleContextConfig cfg);

   void noteEditorAdded(BaleWindow win);

   void noteEditorRemoved(BaleWindow win);

}	// end of inner interface BaleContextHandler



/********************************************************************************/
/*										*/
/*	Language-specific command interface					*/
/*										*/
/********************************************************************************/

interface BaleLanguageKit {

   Action [] getActions();
   Keymap getKeymap(Keymap base);


}	// end of inner class BaleLanguageKit



/********************************************************************************/
/*										*/
/*	Generic find bar							*/
/*										*/
/********************************************************************************/

interface BaleFinder {

   Component getComponent();

   void setReplace(boolean fg);

   void find(int dir,boolean next);

}



/********************************************************************************/
/*										*/
/*	Interfaces for use in AutoCorrection interfaces 			*/
/*										*/
/********************************************************************************/

interface BaleWindow {
   BaleWindowDocument getWindowDocument();
   void addCaretListener(CaretListener cl);
   void removeCaretListener(CaretListener cl);
   BudaBubble getBudaBubble();
   Collection<String> getKeywords();
}	// end of inner interface BaleWindow


interface BaleWindowDocument {
   void addDocumentListener(DocumentListener dl);
   void removeDocumentListener(DocumentListener dl);
   File getFile();
   int mapOffsetToJava(int offset);
   int mapOffsetToEclipse(int offset);
   int findLineNumber(int offset);
   int findLineOffset(int line);
   String getWindowText(int offset,int length);
   String getProjectName();
   BoardLanguage getLanguage();
   BaleFileOverview getBaseWindowDocument();
   BaleWindowElement getCharacterElement(int offset);
   List<BumpProblem> getProblemsAtLocation(int offset);
   boolean replace(int off,int len,String text,boolean fmt,boolean ind);
}	// end of inner interface BaleWindowDocument

interface BaleWindowElement {
   boolean isIdentifier();
   BaleWindowElement getNextCharacterElement();
   BaleWindowElement getPreviousCharacterElement();
   boolean isTypeIdentifier();
   int getStartOffset();
   int getEndOffset();
   BaleTokenType getTokenType();
   BaleWindowElement getBaleParent();
   String getName();
}	// end of inner interface BaleWindowElement


}	// end of interface BaleConstants




/* end of BaleConstants.java */


