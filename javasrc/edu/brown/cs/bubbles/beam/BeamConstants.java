/********************************************************************************/
/*										*/
/*		BeamConstants.java						*/
/*										*/
/*	Bubble Environment Auxilliary & Missing items constants 		*/
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


/* RCS: $Header$ */

/*********************************************************************************
 *
 * $Log$
 *
 ********************************************************************************/


package edu.brown.cs.bubbles.beam;

import edu.brown.cs.bubbles.board.BoardFont;

import java.awt.Font;


interface BeamConstants {


/********************************************************************************/
/*										*/
/*	Constants for defining Note bubbles					*/
/*										*/
/********************************************************************************/

/**
 *	Color at the top of a note bubble
 **/
String NOTE_TOP_COLOR_PROP = "Beam.note.top.color";

/**
 *	Color at the bottom of a note bubble
 **/
String NOTE_BOTTOM_COLOR_PROP = "Beam.note.bottom.color";


/**
 *	Color of a note bubble in the overview panel
 **/
String NOTE_OVERVIEW_COLOR_PROP = "Beam.NoteOverviewColor";


/**
 *	Property for the initial width of a note bubble
 **/
String NOTE_WIDTH = "Beam.note.width";


/**
 *	Property for the initial height of a note bubble
 **/
String NOTE_HEIGHT = "Beam.note.height";


/**
 *	Name of the button on the top-level menu for creating note bubbles
 **/
String NOTE_BUTTON = "Bubble.Notes.Create Note";
String NOTE_SELECTOR = "Bubble.Notes.Load Named Note";
String NOTE_COLOR_SELECTOR = "Bubble.Notes.Set Default Color";


/**
 *	Font used in a note bubble
 **/
Font NOTE_FONT = BoardFont.getFont(Font.SERIF,Font.PLAIN,12);

String NOTE_FONT_PROP = "Beam.note.font";



/********************************************************************************/
/*										*/
/*	Flag constants								*/
/*										*/
/********************************************************************************/

/**
 *	Name of the button for creating standard flags on the menu.
 **/
String FLAG_BUTTON = "Bubble.Create Flag.Flag";
String FLAG_IMAGE = "flags/default/Flag.png";



/**
 *	Name of the button for creating fixed flags on the top-level menu.
 **/
String FLAG_FIXED_BUTTON = "Bubble.Create Flag.Create Fixed Flag";
String FLAG_FIXED_IMAGE = "flags/default/Fixed.png";



/**
 *	Name of the button for creating warning flags on the top-level menu.
 **/
String FLAG_WARNING_BUTTON = "Bubble.Create Flag.Create Warning Flag";
String FLAG_WARNING_IMAGE = "flags/default/Warning.png";

/**
 *	Name of the button for creating action flags on the top-level menu.
 **/
String FLAG_ACTION_BUTTON = "Bubble.Create Flag.Other.Create Action Flag";
String FLAG_ACTION_IMAGE = "flags/additional/Action.png";


/**
 *	Name of the button for creating bomb flags on the top-level menu.
 **/
String FLAG_BOMB_BUTTON = "Bubble.Create Flag.Other.Create Bomb Flag";
String FLAG_BOMB_IMAGE = "flags/additional/Bomb.png";

/**
 *	Name of the button for creating bug flags on the top-level menu.
 **/
String FLAG_BUG_BUTTON = "Bubble.Create Flag.Other.Create Bug Flag";
String FLAG_BUG_IMAGE = "flags/additional/Bug.png";

/**
 *	Name of the button for creating clock flags on the top-level menu.
 **/
String FLAG_CLOCK_BUTTON = "Bubble.Create Flag.Other.Create Clock Flag";
String FLAG_CLOCK_IMAGE = "flags/additional/Clock.png";

/**
 *	Name of the button for creating database flags on the top-level menu.
 **/
String FLAG_DATABASE_BUTTON = "Bubble.Create Flag.Other.Create Database Flag";
String FLAG_DATABASE_IMAGE = "flags/additional/Database.png";

/**
 *	Name of the button for creating fish flags on the top-level menu.
 **/
String FLAG_FISH_BUTTON = "Bubble.Create Flag.Other.Create Fish Flag";
String FLAG_FISH_IMAGE = "flags/additional/Fish.png";

/**
 *	Name of the button for creating idea flags on the top-level menu.
 **/
String FLAG_IDEA_BUTTON = "Bubble.Create Flag.Other.Create Idea Flag";
String FLAG_IDEA_IMAGE = "flags/additional/Idea.png";

/**
 *	Name of the button for creating investigate flags on the top-level menu.
 **/
String FLAG_INVESTIGATE_BUTTON = "Bubble.Create Flag.Other.Create Investigate Flag";
String FLAG_INVESTIGATE_IMAGE = "flags/additional/Investigate.png";

/**
 *	Name of the button for creating link flags on the top-level menu.
 **/
String FLAG_LINK_BUTTON = "Bubble.Create Flag.Other.Create Link Flag";
String FLAG_LINK_IMAGE = "flags/additional/Link.png";

/**
 *	Name of the button for creating star flags on the top-level menu.
 **/
String FLAG_STAR_BUTTON = "Bubble.Create Flag.Other.Create Star Flag";
String FLAG_STAR_IMAGE = "flags/additional/Star.png";





/********************************************************************************/
/*										*/
/*	Web constants								*/
/*										*/
/********************************************************************************/

/**
 *	Property for the default width of a web bubble
 **/
String WEB_WIDTH = "Beam.web.width";


/**
 *	Property for the default height of a web bubble
 **/
String WEB_HEIGHT = "Beam.web.height";


/**
 *	Default initial URL for a web bubble
 **/
//String WEB_DEFAULT_URL = "http://www.google.com";
String WEB_DEFAULT_URL = "Beam.web.url";

/**
 *	Name of the web bubble button on the top level menu.
 **/
String WEB_BUTTON = "Bubble.Open Webpage";



/********************************************************************************/
/*										*/
/*	Problem/Task constants							*/
/*										*/
/********************************************************************************/

/**
 *	Name of the button for problem bubbles.
 **/
String PROBLEM_BUTTON = "Bubble.Errors/Warnings";


/**
 *	Color at the top of a problem (error/warning) bubble.
 **/
String PROBLEM_TOP_COLOR_PROP = "Beam.problem.top.color";

/**
 *	Color at the bottom of a problem (error/warning) bubble.
 **/
String PROBLEM_BOTTOM_COLOR_PROP = "Beam.problem.bottom.color";

/**
 *	Color to use in the overview area for a problem bubble.
 **/
String PROBLEM_OVERVIEW_COLOR_PROP = "Beam.ProblemOverviewColor";


/**
 *	Color for displaying error messages in a problem bubble.
 **/
String PROBLEM_ERROR_COLOR_PROP = "Beam.ProblemErrorColor";


/**
 *	Color for displaying warning messages in a problem bubble.
 **/
String PROBLEM_WARNING_COLOR_PROP = "Beam.ProblemWarningColor";


/**
 *	Color for displaying notice messages (e.g. TODOs) in a problem/task bubble
 **/
String PROBLEM_NOTICE_COLOR_PROP = "Beam.ProblemNoticeColor";



/**
 *	Property name for the initial width of a problem bubble.
 **/
String PROBLEM_WIDTH="Beam.problem.width";
//int PROBLEM_WIDTH = 400;


/**
 *	Property name for the initial height of a problem bubble.
 **/
String PROBLEM_HEIGHT="Beam.problem.height";
//int PROBLEM_HEIGHT = 150;



/**
 *	Name on the top-level menu for the button to create a task bubble.
 **/
String TASK_BUTTON = "Bubble.Todo Tasks";


/**
 *	Color at the top of a task bubble.
 **/
String TASK_TOP_COLOR_PROP = "Beam.TaskTopColor";


/**
 *	Color at the bottom of a task bubble.
 **/
String TASK_BOTTOM_COLOR_PROP = "Beam.TaskBottomColor";


/**
 *	Color of a task bubble in the overview area.
 **/
String TASK_OVERVIEW_COLOR_PROP = "Beam.TaskOverviewColor";




/**
 *	Name for the bubbles web page button
 **/
String HELP_HOME_BUTTON = "Bubble.Help.Show Code Bubbles Home Page";

/**
 *	Name for the help video button
 **/
String HELP_VIDEO_BUTTON = "Bubble.Help.Show Help Video";

/**
 *	Button name for wiki page
 **/
String HELP_WIKI_BUTTON = "Bubble.Help.Show Code Bubbles Wiki Page";

String HELP_TUTORIAL_BUTTON = "Bubble.Help.Show Bubbles Tutorial";

String HELP_KEY_BUTTON = "Bubble.Help.Show Key Bindings";

/**
 *	URL of the help video
 **/
String HELP_VIDEO_URL = "http://www.cs.brown.edu/people/spr/codebubbles/demovideo.mov";
String HELP_VIDEO_KEY = "Beam.help.video";
String HELP_HOME_URL = "http://www.cs.brown.edu/people/spr/codebubbles/";
String HELP_HOME_KEY = "Beam.help.home";
String HELP_WIKI_URL = "http://conifer2.cs.brown.edu:8000/bubbles/wiki/OverView";
String HELP_WIKI_KEY = "Beam.help.wiki";
String HELP_TUTORIAL_URL = "http://www.cs.brown.edu/people/spr/codebubbles/tutorial";
String HELP_TUTORIAL_KEY = "Beam.help.tutorial";
String HELP_TODO_URL = "http://www.cs.brown.edu/people/spr/codebubbles/todohelp.html";
String HELP_TODO_KEY = "Beam.help.todo";
String HELP_MAIL_URL = "spr+bubbles@cs.brown.edu";
String HELP_MAIL_KEY = "Beam.help.mailto";




/********************************************************************************/
/*										*/
/*	Toolbar names								*/
/*										*/
/********************************************************************************/

String BEAM_TOOLBAR_MENU_BUTTON = "DefaultMenu";

String PALETTE_BUTTON = "Admin.Admin.Set Color Palette";





}	 // end in interface BeamConstants





/* end of BeamConstants.java */

