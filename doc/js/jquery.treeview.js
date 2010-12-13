/*
 * Treeview 1.2 - jQuery plugin to hide and show branches of a tree
 *
 * Copyright (c) 2006 Jörn Zaefferer, Myles Angell
 *
 * Dual licensed under the MIT and GPL licenses:
 *   http://www.opensource.org/licenses/mit-license.php
 *   http://www.gnu.org/licenses/gpl.html
 *
 * Revision: $Id$
 *
 */

/**
 * Takes an unordered list and makes all branches collapsable.
 *
 * The "treeview" class is added if not already present.
 *
 * To hide branches on first display, mark their li elements with
 * the class "closed". If the "collapsed" option is used, mark intially open
 * branches with class "open".
 *
 * @example .treeview, .treeview ul { 
 * 	padding: 0;
 * 	margin: 0;
 * 	list-style: none;
 * }	
 * 
 * .treeview li { 
 * 	margin: 0;
 * 	padding: 4px 0 3px 20px;
 * }
 * 
 * .treeview li { background: url(images/tv-item.gif) 0 0 no-repeat; }
 * .treeview .collapsable { background-image: url(images/tv-collapsable.gif); }
 * .treeview .expandable { background-image: url(images/tv-expandable.gif); }
 * .treeview .last { background-image: url(images/tv-item-last.gif); }
 * .treeview .lastCollapsable { background-image: url(images/tv-collapsable-last.gif); }
 * .treeview .lastExpandable { background-image: url(images/tv-expandable-last.gif); }
 * @desc The following styles are necessary in your stylesheet. There is are alternative sets of images available.
 *
 * @example $("ul").Treeview();
 * @before <ul>
 *   <li>Item 1
 *     <ul>
 *       <li>Item 1.1</li>
 *     </ul>
 *   </li>
 *   <li class="closed">Item 2 (starts closed)
 *     <ul>
 *       <li>Item 2.1
 *         <ul>
 *           <li>Item 2.1.1</li>
 *           <li>Item 2.1.2</li>
 *         </ul>
 *       </li>
 *       <li>Item 2.2</li>
 *     </ul>
 *   </li>
 *   <li>Item 3</li>
 * </ul>
 * @desc Basic usage example
 *
 * @example $("ul").Treeview({ speed: "fast", collapsed: true});
 * @before <ul>
 *   <li class="open">Item 1 (starts open)
 *     <ul>
 *       <li>Item 1.1</li>
 *     </ul>
 *   </li>
 *   <li>Item 2
 *     <ul>
 *       <li>Item 2.1</li>
 *       <li>Item 2.2</li>
 *     </ul>
 *   </li>
 * </ul>
 * @desc Create a treeview that starts collapsed. Toggling branches is animated.
 *
 * @example $("ul").Treeview({ control: #treecontrol });
 * @before <div id="treecontrol">
 *   <a href="#">Collapse All</a>
 *   <a href="#">Expand All</a>
 *   <a href="#">Toggle All</a>
 * </div>
 * @desc Creates a treeview that can be controlled with a few links.
 * Very likely to be changed/improved in future versions.
 *
 * @param Map options Optional settings to configure treeview
 * @option String|Number speed Speed of animation, see animate() for details. Default: none, no animation
 * @option Boolean collapsed Start with all branches collapsed. Default: none, all expanded
 * @option <Content> control Container for a treecontrol, see last example.
 * @option Boolean unique Set to allow only one branch on one level to be open
 *		   (closing siblings which opening). Default: none
 * @option Function toggle Callback when toggling a branch.
 * 		   Arguments: "this" refers to the UL that was shown or hidden.
 * 		   Works only with speed option set (set speed: 1 to enable callback without animations).
 *		   Default: none
 * @type jQuery
 * @name Treeview
 * @cat Plugins/Treeview
 */

(function($) {

	// classes used by the plugin
	// need to be styled via external stylesheet, see first example
	var CLASSES = {
		open: "open",
		closed: "closed",
		expandable: "expandable",
		collapsable: "collapsable",
		lastCollapsable: "lastCollapsable",
		lastExpandable: "lastExpandable",
		last: "last",
		hitarea: "hitarea"
	};
	
	// styles for hitareas
	var hitareaCSS = {
		height: 15,
		width: 30, // custom size used in xooki
		marginLeft: "-30px", // custom size used in xooki
		"float": "left",
		cursor: "pointer"
	};
	
	// ie specific styles for hitareas
	if( $.browser.msie ) {
		$.extend( hitareaCSS, {
			background: "#fff",
			filter: "alpha(opacity=0)",
			display: "inline"
		});
	}

	$.extend($.fn, {
		swapClass: function(c1, c2) {
			return this.each(function() {
				var $this = $(this);
				if ( $.className.has(this, c1) )
					$this.removeClass(c1).addClass(c2);
				else if ( $.className.has(this, c2) )
					$this.removeClass(c2).addClass(c1);
			});
		},
		replaceclass: function(c1, c2) {
			return this.each(function() {
				var $this = $(this);
				if ( $.className.has(this, c1) )
					$this.removeClass(c1).addClass(c2);
			});
		},
		Treeview: function(settings) {
		
			// currently no defaults necessary, all implicit
			settings = $.extend({}, settings);
		
			// factory for treecontroller
			function treeController(tree, control) {
				// factory for click handlers
				function handler(filter) {
					return function() {
						// reuse toggle event handler, applying the elements to toggle
						// start searching for all hitareas
						toggler.apply( $("div." + CLASSES.hitarea, tree).filter(function() {
							// for plain toggle, no filter is provided, otherwise we need to check the parent element
							return filter ? $(this).parent("." + filter).length : true;
						}) );
						return false;
					}
				}
				// click on first element to collapse tree
				$(":eq(0)", control).click( handler(CLASSES.collapsable) );
				// click on second to expand tree
				$(":eq(1)", control).click( handler(CLASSES.expandable) );
				// click on third to toggle tree
				$(":eq(2)", control).click( handler() ); 
			}
		
			// handle toggle event
			function toggler() {
				// this refers to hitareas, we need to find the parent lis first
				$( this ).parent()
					// swap classes
					.swapClass( CLASSES.collapsable, CLASSES.expandable )
					.swapClass( CLASSES.lastCollapsable, CLASSES.lastExpandable )
					// find child lists
					.find( ">ul" )
					// toggle them
					.toggle( settings.speed, settings.toggle );
				if ( settings.unique ) {
					$( this ).parent()
						.siblings()
						.replaceclass( CLASSES.collapsable, CLASSES.expandable )
						.replaceclass( CLASSES.lastCollapsable, CLASSES.lastExpandable )
						.find( ">ul" )
						.hide( settings.speed, settings.toggle );
				}
			}
	
			// add treeview class to activate styles
			this.addClass("treeview");
			
			// mark last tree items
			$("li:last-child", this).addClass(CLASSES.last);
			
			// collapse whole tree, or only those marked as closed, anyway except those marked as open
			$( (settings.collapsed ? "li" : "li." + CLASSES.closed) + ":not(." + CLASSES.open + ") > ul", this).hide();
			
			// find all tree items with child lists
			$("li[ul]", this)
				// handle closed ones first
				.filter("[>ul:hidden]")
					.addClass(CLASSES.expandable)
					.swapClass(CLASSES.last, CLASSES.lastExpandable)
					.end()
				// handle open ones
				.not("[>ul:hidden]")
					.addClass(CLASSES.collapsable)
					.swapClass(CLASSES.last, CLASSES.lastCollapsable)
					.end()
				// append hitarea
				.prepend("<div class=\"" + CLASSES.hitarea + "\">")
				// find hitarea
				.find("div." + CLASSES.hitarea)
				// apply styles to hitarea
				.css(hitareaCSS)
				// apply toggle event to hitarea
				.toggle( toggler, toggler );
			
			// if control option is set, create the treecontroller
			if ( settings.control )
				treeController(this, settings.control);
			
			return this;
		}
	});
})(jQuery);