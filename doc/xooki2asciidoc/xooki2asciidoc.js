/*
	Copyright (c) 2006-2007, The Xooki project
	http://xooki.sourceforge.net/

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
	
	Some code is largely inspired by code found in the dojo toolkit, 
	see http://dojotoolkit.org/ for more information.
*/

/*
This script can be either embedded in a xooki page for in browser processing, or used in batch using rhino or java 6 javascript tool:
jrunscript path/to/xooki.js inputFileFromXookiSite.html [path/to/dir/to/put/generated/file]

Be sure to be in the directory where the html input to process is when running this command.
 */
var batchMode = (typeof arguments != 'undefined');

var xooki = {};
xooki.console = ""; // used for debugging purpose only, and only when the debug div is not yet created
xooki.config = {};
xooki.c = xooki.config;

function t(msg) {
    // returns the internationalized version of the message, or the message if no translation is available
    // t stands for translate
// FIXME
//    if (typeof xooki.c == "object" 
//        && typeof xooki.c.messages == "object" 
//        && typeof xooki.c.messages[msg] == "string") {
//        msg = xooki.c.messages[msg];
//    }
    var arr = [];
    for (var i=1; i<arguments.length; i++) {
        arr.push(arguments[i]);
    }
    return xooki.string.substituteParams(msg, arr);
}

function css(clss) {
    // returns the css class or id configured, or the given class (or id) if no one is configured
    if (typeof xooki.c.css[clss] != "undefined") {
        return xooki.c.css[clss];
    } else {
        return clss;
    } 
}
function u(path) {
  // convert a path relative to the root to a full URL
  // u stands for Url
  if (batchMode) {
	return xooki.c.relativeRoot+path;
  } else {
	return xooki.c.root + path;
  }
}
function lu(path) {
  // convert a path relative to the local root to a full URL
  // l stands for local, u stands for Url
  if (batchMode) {
	return xooki.c.localRelativeRoot+path;
  } else {
	return xooki.c.localRoot + path;
  }
}
function cu(urlCfgProp) {
  // get a path from a configuration path and convert it to an URL
  // cu stands for Configured Url
  if (typeof xooki.c.path[urlCfgProp] == "undefined") {
    xooki.warn(t("path not configured in xooki: '${0}'", urlCfgProp));
    return "";
  }
  return u(xooki.c.path[urlCfgProp]);
}
function pu(id) {
  // returns the url of the page identified by id
  // pu stands for Page Url
  return u(id+".html");
}
xooki.p = function(path) {
  // get a xooki full path from a xooki relative path
  // p stands for path
  return xooki.c.path.install+"/"+path;
}
xooki.u = function(path) {
  // convert a path relative to the xooki installation dir to a full URL
  return u(xooki.p(path));
}
xooki.cu = function(urlCfgProp) {
  // get a xooki path from a configuration path and convert it to an URL
  if (typeof xooki.c.path[urlCfgProp] == "undefined") {
    xooki.warn(t("path not configured in xooki: '${0}'", urlCfgProp));
  }
  return xooki.u(xooki.c.path[urlCfgProp]);
}

xooki.util = {
    isArray: function(it) {
    	return (it && it instanceof Array || typeof it == "array"); // Boolean
    },
    mix: function(src, into, override) {
        if (typeof override == "undefined") {
            override = true;
        }
        if (typeof into == "undefined") {
            into = {};
        }
        for (var k in src) {
            if (typeof src[k] == "object" && !xooki.util.isArray(src[k])) {
                if (override || typeof into[k] == "object" || typeof into[k] == "undefined") {
                    if (typeof into[k] != "object") {
                        into[k] = {};
                    }
                    xooki.util.mix(src[k], into[k], override);
                }
            } else if (override || typeof into[k] == "undefined") {
                into[k] = src[k];
            }
        }
        return into;
    },
    initArray: function(a) {
        if (this.isArray(a)) return a;
        else return {};
    }
}

xooki.url = {
        newXmlHttpRequest: function() {
        // we first try to use ActiveX, because IE7 has a direct support for XmlHttpRequest object, 
        // but which doesn't work with file urls
        	if(window.ActiveXObject)
        	{
        		try { req = new ActiveXObject("Msxml2.XMLHTTP");
        		} catch(e) {
        			try { req = new ActiveXObject("Microsoft.XMLHTTP");
        			} catch(e) { req = false; }
        		}
        	}
        	else if(window.XMLHttpRequest) {
        		try { req = new XMLHttpRequest();
        		} catch(e) { req = false; }
        	}
        
        	return req;	
        },
                
        loadURL: function( url, warnOnError ) {
        	req = this.newXmlHttpRequest();
        	if(req) {
        		try {
        			req.open("GET", url, false);
        			req.send("");
        	
        			return req.responseText;
        		} catch (e) {
                    if (warnOnError != false)
                        xooki.error(e, t("problem while loading URL ${0}", url));
                    else
                        xooki.debug(t("problem while loading URL ${0}: ${1}", url, e));
                }		
        	}
        	return null;
        },
        
        asyncLoadURL: function( url, callback, obj ) {
        	var req = this.newXmlHttpRequest();
        	if(req) {
        		try {
        			req.open("GET", url, true);
				    req.onreadystatechange=function() {
				        if (req.readyState == 4) {
				           if (req.status == 200 || req.status == 0) {
				              callback(req.responseText, obj);
				           }
				        }
				     };  			
				     req.send("");
        		} catch (e) {
        			xooki.error(e, t("problem while loading URL ${0}", url));
                }		
        	}
        },

        include: function(script_filename) {
            document.write('<' + 'script');
            document.write(' language="javascript"');
            document.write(' type="text/javascript"');
            document.write(' src="' + xooki.u(script_filename) + '">');
            document.write('</' + 'script' + '>');
        },
        
        evalURL: function( url, warnOnErrorUrl ) {
            script = this.loadURL(url, warnOnErrorUrl);
            if (script != null) {
                try {
                    eval(script);
                } catch (e) {
        			xooki.error(e, t("error while executing script from URL ${0}", url));
                }
            }
        },

        action: function(action) {        
            // returns the url for an action on the same page
            loc = batchMode?'':xooki.pageURL;
            if (loc.indexOf("#") != -1) {
                loc = loc.substring(0, loc.indexOf("#"));
            }
            return loc+"?action="+action;
        }

    };
    
xooki.string = {

    substituteParams: function(/*string*/template, /* object - optional or ... */hash) {
    // borrowed from dojo
    // summary:
    //	Performs parameterized substitutions on a string. Throws an exception if any parameter is unmatched.
    //
    // description:
    //	For example,
    //		dojo.string.substituteParams("File '${0}' is not found in directory '${1}'.","foo.html","/temp");
    //	returns
    //		"File 'foo.html' is not found in directory '/temp'."
    //
    // template: the original string template with ${values} to be replaced
    // hash: name/value pairs (type object) to provide substitutions.  Alternatively, substitutions may be
    //	included as an array
    
        var map;
        if (typeof hash == "object" && hash.length) { // array
            map = {};
            for (var i in hash) {
                map[i+""] = hash[i];
            }
        } else {
            map = hash;
        }
    
    	return template.replace(/\$\{(\w+)\}/g, function(match, key){
    		if(typeof(map[key]) != "undefined" && map[key] != null){
    			return map[key];
    		}
    		xooki.warn("Substitution not found: " + key);
    		return key;
    	}); // string
	},
	
	processTemplate: function(/*string*/template, /* object */hash) {
	   if (typeof template.process == "function") {
	       return template.process(hash);
       } else {
            return this.substituteParams(template, hash);
       }
    },

    exceptionText: function(e, message) {
    	var s = e.description ? e.description : e.toString();
    	return message ? message+":\n"+s : s;
    },
    
    findXmlSection: function(str, element, from) {
        return this.findSection(str, new RegExp('<'+element+'(\\s*\\w+="[^\\"]*")*>'), new RegExp('</'+element+'>'), from);
    },
    
    find: function(/*string*/str, /*string or regexp*/exp, /*number, optional*/from) {
        // find an expression (string or regexp) in a string, from an optional index
        // the object returned has two properties:
        // begin: the index in str of the matching find
        // end: the index in str of the end of the matching find
        // returns null if no match is found
        if (typeof from != "number") {
            from = 0;
        }
        if (typeof exp == "string") {
            var result = {};
            result.begin = str.indexOf(exp,from);
            if (result.begin >= 0) {
                result.end = result.begin + exp.length;
                return result;
            }
        } else {
            var m;
            if (from > 0) {
                // I haven't found any other way to start from the given index
                m = exp.exec(str.substring(from));
            } else {
                m = exp.exec(str);
            }
            if (m != null) {                
                var result = {};
                result.begin = m.index + from;
                result.end = result.begin + m[0].length;
                result.matcher = m;
                return result;
            }
        }
        return null;
    },
    
    findSection: function(/*string*/str, /*string or regexp*/open, /*string or regexp*/close, /*number, optional*/from) {
        // finds a section delimited by open and close tokens in the given string
        // the algorithm looks for matching open and close tokens
        // the returned object has the following properties:
        //   outerStart: the index in str where the first open token was found
        //   innerStart: the index in str just after the found open token
        //   innerEnd: the index in str where the matching close token was found
        //   outerEnd: the index in str just after the matching close token
        //   children: an array of similar objects if nested sections where found
        // if no section is found (no open token, an open token with no matching 
        // close token, or a close token before an open token), null is returned
        //
        // for instance if open=='(' and close==')' then the section will find
        // a section delimited by the first found open parenthesis and the matching
        // close parentethis, taking into account other opening parentethis
        // examples:
        // findSection("a(test)b", "(", ")") == {outerStart: 1, innerStart:2, innerEnd:6, outerEnd:7, children:[]}
        // findSection("a(te(s)(t))b", "(", ")") == {outerStart: 1, innerStart:2, innerEnd:10, outerEnd:11, 
        //      children:[
        //          {outerStart: 4, innerStart:5, innerEnd:6, outerEnd:7, children:[]},
        //          {outerStart: 7, innerStart:8, innerEnd:9, outerEnd:10, children:[]}
        //      ]}
        
        var openResult = this.find(str, open, from);        
        if (openResult == null) {
            print('no open\n');
            return null;
        }
        if (openResult.end <= openResult.begin) {
            print('empty open\n');
            // empty match are not allowed
            return null;
        }
        var closeResult = this.find(str, close, openResult.end);        
        if (closeResult == null) {
            print('no close\n');
            return null;
        }
        if (closeResult.end <= closeResult.begin) {
            print('empty close\n');
            // empty match are not allowed
            return null;
        }
        
        //print('matched !' + str.substring(openResult.begin, closeResult.end) + '\n');

        var children = [];
        var child = this.findSection(str, open, close, openResult.end);
        while (child != null) {
            if (child.outerStart > closeResult.begin) {
                break;
            }
            if (child.outerEnd > closeResult.begin) {
                closeResult = this.find(str, close, child.outerEnd);        
                if (closeResult == null) {
                    // unmatched open token
                    return null;
                }
            }
            children.push(child);
            child = this.findSection(str, open, close, child.outerEnd);
        }

        //print('found !' + str.substring(openResult.begin, closeResult.end) + '\n');

        return {
            outerStart: openResult.begin,
            innerStart: openResult.end,
            matcherStart: openResult.matcher,
            innerEnd: closeResult.begin,
            outerEnd: closeResult.end,
            matcherEnd: closeResult.matcher,
            children: children
        };        
    },
    
    mul: function (/*string*/ s, /*int*/ n) {
        r = '';
        for (var i=0; i < n; i++) {
    		r += s;
    	}
        return r;
    }
};
    
xooki.json = {
        evalJson: function (str) {
            try {
                return eval("("+str+")");
            } catch (e) {
                return null;
            }
        },
        
        loadURL: function (url) {
            return this.evalJson(xooki.url.loadURL(url));
        }
    };

// Displays an alert of an exception description with optional message
xooki.warn = function(e, message) {
    xooki.display(xooki.string.exceptionText(e, message), "#eecccc");
}

// Displays an alert of an exception description with optional message
xooki.error = function(e, message) {
    xooki.display(xooki.string.exceptionText(e, message), "#ffdddd");
}

xooki.info = function(message) {
    xooki.display(message, "#ddddff");
}

xooki.display = function(message, background) {
    var messages = document.getElementById('xooki-messages');
    if (messages) {
        messages.innerHTML = '<table width="100%" border="0"><tr><td align="center">'+message+'</td></tr></table>';
        messages.style.background = background;
        messages.style.display = "inline";
    } else {
        alert(message);
    }
}

xooki.debug = function(message) {
    var console = typeof document == 'undefined' ? false : document.getElementById('xooki-console');
    if (console) {
        console.value += message + "\n";
    } else {
        xooki.console += message + "\n";
    }
}

xooki.debugShowDetail = function (message) {
    var detail = typeof document == 'undefined' ? false : document.getElementById('xooki-debug-detail');
    if (detail) {
        detail.value=message;
    } else {
        alert(message);
    }
}


xooki.html = {
    hide: function(divid) {
	   document.getElementById(divid).style.display = 'none';
    },

    show: function (divid) {
	   document.getElementById(divid).style.display = '';
    },
    
    pageLink: function(page) {
    	if (page.isAbstract) {
    		return page.title;
    	} else {
    		return '<a href="'+(page.url != null ? page.url : pu(page.id))+'" '+(page.id == xooki.page.id?'class="current"':'')+'>'+page.title+'</a>';
    	}
    },
	
	// insert  the given  header in the html head
	// can be used only when the browser is still in the head !
	addHeader: function(/* string */ head) {
		document.write(head);
	},
	
	setBody: function( /* string */ body) {
		document.body.innerHTML = body;
	}
};

xooki.component = {
    childrenList: function () {
    	if (xooki.page.children.length > 0) {
    		childrenList = '<ul class="'+css('childrenList')+'">\n';
    		for (var i in xooki.page.children) {
    			childrenList+='<li>'+xooki.html.pageLink(xooki.page.children[i])+'</li>\n';
    		}
    		childrenList += "</ul>\n";
    		return childrenList;
    	} else {
    	   return "";
        }
    },
    
    menu: function () {
    	var menu = '<ul id="'+css("treemenu")+'" class="treeview">\n';
    	menu += (function (page) {
        	var menu = '';
        	for (var i in page.children) {
                if (typeof page.children[i] == 'object') {
            		smenu = arguments.callee(page.children[i]);
            		if (smenu != '') {
                        menu += '<li id="xooki-'+page.children[i].id+'" class="submenu">'+xooki.html.pageLink(page.children[i]);
            			if (smenu.indexOf('id="xooki-'+xooki.page.id+'"') != -1 
            				|| page.children[i].id == xooki.page.id) {
            				// either a descendant or the node processed is the current page node
            				// we specify that the menu must be opened by default
            				menu += '<ul class="open"';
    	        			menu += '>'+smenu+'</ul>';
            			} else {
    						menu += '<ul class="closed"';
    	        			menu += '>'+smenu+'</ul>';
    					}
            		} else {
                        menu += '<li id="xooki-'+page.children[i].id+'">'+xooki.html.pageLink(page.children[i]);
                    }
            		menu += '</li>\n';
                }
        	}
        	return menu;
        })(xooki.toc);
    	menu += '</ul>\n';
    	return menu;
    },
    
    messages: function () {
        return '<div id="xooki-messages" onclick="xooki.html.hide(\'xooki-messages\')" style="zIndex:999;display:none;position:absolute;top:30px;padding:10px;border-style:solid;background:#eeeeee;"></div>';
    },

    debugPanel: function () {
        return '<div id="xooki-debug" style="display:none;margin-top:15px;padding:10px;border-style:solid;background:#eeeeee;"><strong>Xooki Console</strong><br/><textarea cols="100" rows="15" id="xooki-console">'+xooki.console+'</textarea><hr/><a href="javascript:xooki.debugShowDetail(document.getElementById(\'xooki-body\').innerHTML)">content</a> <a href="javascript:xooki.debugShowDetail(xooki.c.body)">xooki body</a> <a href="javascript:xooki.debugShowDetail(document.body.innerHTML)">whole body</a> <a href="javascript:xooki.action.evaluate()">evaluate</a><br/><textarea cols="100" rows="15" id="xooki-debug-detail"></textarea></div>';
    },

    printerFriendlyLocation: function () {
        return xooki.url.action("print");
    },

    printerFriendlyLink: function () {
        return '<a href="'+this.printerFriendlyLocation()+'">'+t('Printer Friendly')+'</a>';
    },

    breadCrumb: function () {
        var breadCrumb = '<span class="breadCrumb">';
		breadCrumb += (function (page) {
        	var breadCrumb = xooki.html.pageLink(page);
			if (page.meta.level >= 1) {
				breadCrumb = arguments.callee(page.meta.parent) + " &gt; " + breadCrumb;
			}
        	return breadCrumb;
        })(xooki.page);
		breadCrumb += '</span>';
		return breadCrumb;
    }
};

xooki.render = {};
xooki.render.printerFriendlyAsyncLoader = function(source, arr) {
	var root = arr[0];
	var page = arr[1];
    if (source == null) {
        return;
    }
	var level = page.meta.level - root.meta.level + 1;
	
    // compute printer friendly block
    var beginIndex = source.indexOf('<textarea id="xooki-source">');
    beginIndex += '<textarea id="xooki-source">'.length;
    var endIndex = source.lastIndexOf('</textarea>');
    source = source.substring(beginIndex, endIndex);
    
    var printerFriendly = "<h"+level+">"+page.title+"</h"+level+">";
    printerFriendly += xooki.input.format.main(source, level) + "<hr/>";
    // inject block in page
    var pf = document.getElementById('xooki-printerFriendly');
    pf.innerHTML += printerFriendly;    
    
    // continue recursive loading
   	var nextPage = xooki.toc.getNextPage(page, root);
   	if (nextPage != null) {
    	xooki.url.asyncLoadURL(pu(nextPage.id), xooki.render.printerFriendlyAsyncLoader, [root, nextPage]);
   	}
};

xooki.render.printerFriendlyAsync = function() {
	xooki.c.body = xooki.c.messages
	+ "<div id='xooki-printerFriendly'></div>" // div where printer friendly content will be put
    + xooki.c.debugPanel;
    
    document.body.innerHTML = xooki.string.processTemplate(xooki.template.body, xooki.c);
    
    // start async loading of content
    xooki.url.asyncLoadURL(pu(xooki.page.id), xooki.render.printerFriendlyAsyncLoader, [xooki.page, xooki.page]);
};

xooki.render.printerFriendlySync = function() {
	xooki.c.body = xooki.c.messages
    + (function (page, level) {
        var source = xooki.url.loadURL(pu(page.id));
        if (source == null) {
            return "";
        }
        var beginIndex = source.indexOf('<textarea id="xooki-source">');
        beginIndex += '<textarea id="xooki-source">'.length;
        var endIndex = source.lastIndexOf('</textarea>');
        source = source.substring(beginIndex, endIndex);
        
        var printerFriendly = "<div class='toc-title toc-title-"+level+"'>"+page.title+"</div>";
        printerFriendly += xooki.input.format.main(source, level);
        for (var i=0; i <page.children.length; i++) {
            printerFriendly += "<hr/>";
            printerFriendly += arguments.callee(page.children[i], level+1);
        }
        return printerFriendly;
    })(xooki.page, 1);
    
    xooki.html.setBody(xooki.string.processTemplate(xooki.template.body, xooki.c));
};

xooki.render.printerFriendly = function() {
    for (var k in xooki.component) {
        xooki.c[k] = xooki.component[k]();
    }
    
	if (batchMode) {
		xooki.render.printerFriendlySync();
	} else {
		xooki.render.printerFriendlyAsync();
	}
};


xooki.render.page = function() {
    // realize all components available
    for (var k in xooki.component) {
        xooki.c[k] = xooki.component[k]();
    }
    
    xooki.input.source();
    
    if (xooki.c.allowEdit) {
    	xooki.c.body = xooki.c.messages
            + xooki.c.toolbar
            + '<div id="xooki-content">' 
                + '<div id="xooki-body"></div>'
            + '</div>'
            + xooki.c.editZone
            + xooki.c.debugPanel;
    } else {
    	xooki.c.body = xooki.c.messages
            + '<div id="xooki-content">' 
                + '<div id="xooki-body"></div>'
            + '</div>'
            + xooki.c.debugPanel;
    }

    xooki.html.setBody(xooki.string.processTemplate(xooki.template.body, xooki.c));
    
    xooki.input.applyChanges();
};

xooki.render.main = function() {
    if (xooki.c.action == "print") {
        // render the printer friendly version of the page
        this.printerFriendly();
    } else {
        // render the page normally
        this.page();
    }
};

xooki.input = {
    source: function() {
        if (typeof document != 'undefined' && document.getElementById('xooki-source') != null) {
            this._source = document.getElementById('xooki-source').value;
        }
        return this._source;
    },
    processed: function() {
        return this.format.main(this.source());
    },
    
    format: {
        getInputFilters: function (inputFormat) {
            return xooki.c[inputFormat+"InputFormat"];
        },
        define: function (inputFormat, filters) {
            // define a new inputFormat
            // inputFormat: the new input format name
            // filters: an array of input filter names
            xooki.c[inputFormat+"InputFormat"] = filters;
        },
        main: function(source, level) {
            // formats an input source
            if (xooki.c.inputFormat && typeof this.getInputFilters(xooki.c.inputFormat) != "undefined") {
                format = xooki.c.inputFormat;
            } else {
                format = xooki.c.defaultInputFormat;
            }
            filters = this.getInputFilters(format);
            for (var i in filters) {
                if (typeof filters[i] == 'string') {
    				xooki.debug('processing filter '+filters[i]);
                    f = xooki.input.filters[filters[i]];
                    if (typeof f == "function") {
                    	try {
                        	source = f(source, level); // process filter
                        } catch (e) {
    	                    xooki.error(e, t("error occurred while processing filter ${0}", filters[i]));
                        }
                    } else {
                        xooki.error(t("unknown filter ${0} used in input format ${1}", filters[i], format));
                    }
                }
            }
            return source;
        }
    }, 
    
    filters: {
        shortcuts: function (input) {
            // handle shortcut links like this:
            //    [[svn:build.xml]] => <a href="https://xooki.svn.sourceforge.net/svnroot/xooki/trunk/build.xml">build.xml</a>
            //    [[svn:test/example.js a good example]] => <a href="https://xooki.svn.sourceforge.net/svnroot/xooki/trunk/test/example.js">a good example</a>
            // needs to be configured in xooki config like this
            //      xooki.c.shortcuts.<any shortcut>.url = base url of the shortcut. 
            //      ex: xooki.c.shortcuts.svn.url = https://xooki.svn.sourceforge.net/svnroot/xooki/trunk/
            return input.replace(new RegExp("\\[\\[([^:\n]+):([^\\]\n]+)\\]\\]", "g"), function (str, prefix, code, offset, s) {
            	if (typeof xooki.c.shortcuts == "undefined" || typeof xooki.c.shortcuts[prefix] == "undefined") {
                    xooki.debug('unknown shortcut '+prefix);
            		return str;
            	}
                var index = code.indexOf(' ');
                var path = index>0?code.substring(0,index):code;
                
                var title = index>0?code.substring(index+1):path;
                var pre = typeof xooki.c.shortcuts[prefix].pre == "undefined"?'':xooki.c.shortcuts[prefix].pre;
                var post = typeof xooki.c.shortcuts[prefix].post == "undefined"?'':xooki.c.shortcuts[prefix].post;
                return 'link:'+pre+path+post+'['+title+']';
            });
        },
        
        xookiLinks: function (input) {
            // handle xooki links like this:
            //    [[page/id]]
            //    [[page/id My Title]]
            return input.replace(new RegExp("\\[\\[([^\\]]+)\\]\\]", "g"), function (str, code, offset, s) {
                var index = code.indexOf(' ');
                var id = (index>0?code.substring(0,index):code);
                
                var title;
                var url;
                var invalid = false;
                
                if (typeof xooki.toc.pages[xooki.toc.importRoot + id] != "undefined") {
               	    title = xooki.toc.pages[xooki.toc.importRoot + id].title;
                    url = pu(xooki.toc.importRoot + id);
               	} else if (xooki.toc.importRoot.length > 0 && typeof xooki.toc.pages[id] != "undefined") {
               	    title = xooki.toc.pages[id].title;
                    url = pu(id);
               	} else {
                    invalid = true;
               		title = code;
               		url = u(id);
               	}
                if (index>0) {
                	title = code.substring(index+1);
                }
                if (invalid) {
                    if (batchMode) {
                        // do not output invalid links as links in batch mode
                        return title;
                    } else {
                        return title+'link:'+url+'[?]';
                    }
                } else {
                    return 'link:'+url+'['+title+']';
                }
            });
        },
        
        wikiMarkup: function (input) { 
            // handle italic
            input = input.replace(new RegExp("\\_([^\n]+)\\_", "g"), "<em>$1</em>");
            
            return input;
        },
        
        jira: function (input) { 
            // auto replace jira issue ids (like IVY-12) by a link to the issue
            // needs to be configured in xooki config like this
            //      xooki.c.jira.ids = an array of jira projects ids (ex: ["IVY", "ANT"])
            //      xooki.c.jira.url = the url of the jira server (ex: "http://issues.apache.org/jira")
            if (typeof xooki.c.jira != "object") {
                return input;
            }
            input = input.replace(new RegExp("(("+xooki.c.jira.ids.join("|")+")-\\d+)([^\"\\d])", "g"), 'link:'+xooki.c.jira.url+'/browse/$1[$1]$3');
            
            return input;
        },
        
        code: function (input) {
            codeSection = xooki.string.findXmlSection(input, "code");
            from = 0;
            while (codeSection != null) {
                processedSection = "\n[source]\n----\n" 
                    + input.substring(codeSection.innerStart, codeSection.innerEnd)
                    + "\n----\n\n";
                input = input.substring(0, codeSection.outerStart)
                    + processedSection
                    + input.substring(codeSection.outerEnd);
                from = codeSection.outerStart + processedSection.length;
    
                codeSection = xooki.string.findXmlSection(input, "code", from);
            }
            return input;
        },
        
        includes: function (input) {
	        //[<url>] replaced by the content of the url
	        result = "";
	        lastStart = 0;
	        nextPos = input.indexOf("[<" , lastStart);
	        while( nextPos > 0 ) {
		        result = result + input.slice(lastStart,nextPos);
		        lastStart = nextPos;
		        nextPos = input.indexOf(">]" , lastStart);
		        result = result + xooki.url.loadURL(lu(input.slice(lastStart+2,nextPos)));
		        lastStart = nextPos + 2;
		        nextPos = input.indexOf("[<" , lastStart);
	        }
            return result + input.slice(lastStart);
        },

        printFormatImgFix: function (input, level) {
			if (level == undefined || level < 3) {
				return input;
			}
			return input.replace(new RegExp('<img +src *= *\\"([^\\"]*)\\"', "g"), function (str, img, offset, s) {
				l = level;
				while (l > 2) {
					if (img.indexOf("../") >= 0) {
						img = img.substring(3);
					} else {
						break;
					}
					l--;
				}
				return '<img src="'+img+'"';
			});
		},

		htmltags: function(input) {
		    print('search href\n')
            var s = xooki.string.findSection(input, new RegExp('<a\\s*href\\s*=\\s*\\"([^\\"]*)\\"[^>]*>'), new RegExp('</a>'));
            var from = 0;
            while (s != null) {
                var href = s.matcherStart[1];
                processedSection = "link:" + href + "[" + input.substring(s.innerStart, s.innerEnd) + "]";
                input = input.substring(0, s.outerStart) + processedSection + input.substring(s.outerEnd);
                from = s.outerStart + processedSection.length;
                s = xooki.string.findSection(input, new RegExp('<a\\s*href\\s*=\\s*\\"([^\\"]*)\\"[^>]*>'), new RegExp('</a>'), from);
            }

            print('search name\n')
            s = xooki.string.findSection(input, new RegExp('<a\\s*name\\s*=\\s*\\"([^\\"]*)\\"[^>]*>'), new RegExp('</a>'));
            from = 0;
            while (s != null) {
                var name = s.matcherStart[1];
                processedSection = "[[" + name + "]]" + input.substring(s.innerStart, s.innerEnd);
                input = input.substring(0, s.outerStart) + processedSection + input.substring(s.outerEnd);
                from = s.outerStart + processedSection.length;
                s = xooki.string.findSection(input, new RegExp('<a\\s*name\\s*=\\s*\\"([^\\"]*)\\"[^>]*>'), new RegExp('</a>'), from);
            }

            print('search img\n')
            s = xooki.string.find(input, new RegExp('<img\\s*src\\s*=\\s*\\"([^\\"]*)\\"\\s*/>'));
            from = 0;
            while (s != null) {
                processedSection = " image:" + s.matcher[1] + "[]"
                input = input.substring(0, s.begin)
                    + processedSection
                    + input.substring(s.end);
                from = s.begin + processedSection.length;
                s = xooki.string.find(input, new RegExp('<img\\s*src\\s*=\\s*\\"([^\\"]*)\\"\\s*/>'), from);
            }

            print('search br\n')
            s = xooki.string.find(input, new RegExp('<br\\s*/>'));
            from = 0;
            while (s != null) {
                processedSection = "\n"
                input = input.substring(0, s.begin)
                    + processedSection
                    + input.substring(s.end);
                from = s.begin + processedSection.length;
                s = xooki.string.find(input, new RegExp('<br\\s*/>'), from);
            }

            print('search b\n')
            s = xooki.string.findXmlSection(input, "b");
            from = 0;
            print("found=" + (s != null) + "\n")
            while (s != null) {
                processedSection = "*" + input.substring(s.innerStart, s.innerEnd) + "*";
                input = input.substring(0, s.outerStart) + processedSection + input.substring(s.outerEnd);
                from = s.outerStart + processedSection.length;
                s = xooki.string.findXmlSection(input, "b", from);
                print("found=" + (s != null) + "\n")
            }

            print('search strong\n')
            s = xooki.string.findXmlSection(input, "strong");
            from = 0;
            print("found=" + (s != null) + "\n")
            while (s != null) {
                processedSection = "*" + input.substring(s.innerStart, s.innerEnd) + "*";
                input = input.substring(0, s.outerStart) + processedSection + input.substring(s.outerEnd);
                from = s.outerStart + processedSection.length;
                s = xooki.string.findXmlSection(input, "strong", from);
                print("found=" + (s != null) + "\n")
            }

            print('search i\n')
            s = xooki.string.findXmlSection(input, "i");
            from = 0;
            while (s != null) {
                processedSection = "__" + input.substring(s.innerStart, s.innerEnd) + "__";
                input = input.substring(0, s.outerStart)
                    + processedSection
                    + input.substring(s.outerEnd);
                from = s.outerStart + processedSection.length;
    
                s = xooki.string.findXmlSection(input, "i", from);
            }

            print('search h1\n')
            s = xooki.string.findXmlSection(input, "h1");
            from = 0;
            while (s != null) {
                processedSection = "\n== " + input.substring(s.innerStart, s.innerEnd) + "\n";
                input = input.substring(0, s.outerStart)
                    + processedSection
                    + input.substring(s.outerEnd);
                from = s.outerStart + processedSection.length;
    
                s = xooki.string.findXmlSection(input, "h1", from);
            }

            print('search h2\n')
            s = xooki.string.findXmlSection(input, "h2");
            from = 0;
            while (s != null) {
                processedSection = "\n=== " + input.substring(s.innerStart, s.innerEnd) + "\n";
                input = input.substring(0, s.outerStart)
                    + processedSection
                    + input.substring(s.outerEnd);
                from = s.outerStart + processedSection.length;
    
                s = xooki.string.findXmlSection(input, "h2", from);
            }

            print('search h3\n')
            s = xooki.string.findXmlSection(input, "h3");
            from = 0;
            while (s != null) {
                processedSection = "\n==== " + input.substring(s.innerStart, s.innerEnd) + "\n";
                input = input.substring(0, s.outerStart)
                    + processedSection
                    + input.substring(s.outerEnd);
                from = s.outerStart + processedSection.length;
    
                s = xooki.string.findXmlSection(input, "h3", from);
            }

            print('search h4\n')
            s = xooki.string.findXmlSection(input, "h4");
            from = 0;
            while (s != null) {
                processedSection = "\n." + input.substring(s.innerStart, s.innerEnd) + "\n";
                input = input.substring(0, s.outerStart)
                    + processedSection
                    + input.substring(s.outerEnd);
                from = s.outerStart + processedSection.length;
    
                s = xooki.string.findXmlSection(input, "h4", from);
            }

            print('search center\n')
            s = xooki.string.findXmlSection(input, "center");
            from = 0;
            while (s != null) {
                processedSection = input.substring(s.innerStart, s.innerEnd);
                input = input.substring(0, s.outerStart)
                    + processedSection
                    + input.substring(s.outerEnd);
                from = s.outerStart + processedSection.length;
    
                s = xooki.string.findXmlSection(input, "center", from);
            }

            print('search tt\n')
            s = xooki.string.findXmlSection(input, "tt");
            from = 0;
            while (s != null) {
                processedSection = "`" + input.substring(s.innerStart, s.innerEnd) + "`";
                input = input.substring(0, s.outerStart)
                    + processedSection
                    + input.substring(s.outerEnd);
                from = s.outerStart + processedSection.length;
    
                s = xooki.string.findXmlSection(input, "tt", from);
            }

            print('search em\n')
            s = xooki.string.findXmlSection(input, "em");
            from = 0;
            while (s != null) {
                processedSection = "\n[NOTE]\n===============================\n" + input.substring(s.innerStart, s.innerEnd) + "\n===============================\n";
                input = input.substring(0, s.outerStart)
                    + processedSection
                    + input.substring(s.outerEnd);
                from = s.outerStart + processedSection.length;
    
                s = xooki.string.findXmlSection(input, "em", from);
            }

            print('search span-since\n')
            s = xooki.string.findSection(input, new RegExp('<span\\s*class\\s*=\\s*\\"\\s*since\\s*\\"[^>]*>'), new RegExp('</span>'));
            from = 0;
            while (s != null) {
                processedSection = "*__" + input.substring(s.innerStart, s.innerEnd) + "__*";
                input = input.substring(0, s.outerStart) + processedSection + input.substring(s.outerEnd);
                from = s.outerStart + processedSection.length;
                s = xooki.string.findSection(input, new RegExp('<span\\s*class\\s*=\\s*\\"\\s*since\\s*\\"[^>]*>'), new RegExp('</span>'), from);
            }

            print('search ul/ol\n')
            var htmllisttag = function(input, from, indent) {
                var sul = xooki.string.find(input, '<ul>', from);
                var sol = xooki.string.find(input, '<ol>', from);
                var endtag;
                var innerindent;
                var s;
                if (sul != null && (sol == null || sul.begin < sol.begin)) {
                    s = sul;
                    innerindent = "*" + indent
                    endtag = "</ul>";
                } else {
                    s = sol;
                    innerindent = "." + indent
                    endtag = "</ol>";
                }
                if (s == null) {
                    return input;
                }
                input = input.substring(0, s.begin) + input.substring(s.end);
                from = s.begin;
                var lastEnd = s.begin;
                var first = true;
                var sli = xooki.string.findXmlSection(input, "li", from);
                while (sli != null) {
                    s = xooki.string.find(input, endtag, from);
                    if (s != null && s.begin < sli.outerStart) {
                        break;
                    }
                    start = sli.outerStart;
                    if (!first) {
                        start = lastEnd;
                    }
                    processedSection = "\n" + innerindent + input.substring(sli.innerStart, sli.innerEnd).replace(/\\s/, ' ');
                    input = input.substring(0, start)
                        + processedSection
                        + input.substring(sli.outerEnd);
                    from = start + processedSection.length;
                    lastEnd = from;
                    sli = xooki.string.findXmlSection(input, "li", from);
                    first = false;
                }
                s = xooki.string.find(input, endtag, from);
                if (s == null) {
                    print(input.substring(from, from + 100));
                    return input;
                }
                input = input.substring(0, s.begin) + input.substring(s.end);
                from = s.begin;
                return htmllisttag(input, from, indent);
            }

            input = htmllisttag(input, 0, " ");

            print('search table\n')
            s = xooki.string.findXmlSection(input, "table");
            from = 0;
            print("found=" + (s != null) + "\n")
            while (s != null) {
                tableContent = input.substring(s.innerStart, s.innerEnd);
                processedSection = "\n"

                print('search tablehead\n')
                s2 = xooki.string.findXmlSection(tableContent, "thead");
                from2 = 0
                print("found=" + (s2 != null) + "\n")
                if (s2 != null) {
                    tableHead = tableContent.substring(s2.innerStart, s2.innerEnd);
                    processedSection += '[options="header"]\n'
                    processedSection += '|=======\n'

                    print('search th\n')
                    s3 = xooki.string.findXmlSection(tableHead, "th");
                    while (s3 != null) {
                        processedSection += "|" + tableHead.substring(s3.innerStart, s3.innerEnd);
                        s3 = xooki.string.findXmlSection(tableHead, "th", s3.outerEnd);
                    }
                    processedSection += "\n"
                    from2 = s2.outerEnd
                } else {
                    processedSection += '|=======\n'
                }

                print('search tr\n')
                s2 = xooki.string.findXmlSection(tableContent, "tr", from2);
                while (s2 != null) {
                    trContent = tableContent.substring(s2.innerStart, s2.innerEnd);

                    print('search td\n')
                    s3 = xooki.string.findXmlSection(trContent, "td");
                    while (s3 != null) {
                        processedSection += "|" + trContent.substring(s3.innerStart, s3.innerEnd);
                        s3 = xooki.string.findXmlSection(trContent, "td", s3.outerEnd);
                    }
                    processedSection += "\n"
                    
                    from2 = s2.outerEnd;
                    s2 = xooki.string.findXmlSection(tableContent, "tr", from2);
                }

                processedSection += '|=======\n'
                
                input = input.substring(0, s.outerStart) + processedSection + input.substring(s.outerEnd);
                from = s.outerStart + processedSection.length;
                s = xooki.string.findXmlSection(input, "table", from);
                print("found=" + (s != null) + "\n")
            }

            return input;
		},
    },
    
    
    applyChanges: function() {
    	document.getElementById('xooki-body').innerHTML = xooki.input.processed();
    }
};


xooki.postProcess = function() {
	xooki.render.main();
	window.onkeypress = keyCtrl;
};


if (typeof xooki.io == "undefined") {
    xooki.io = {};
}


xooki.action = {}
xooki.action.toggleDebug = function() {
    if (xooki.c.debug) {
    	if (document.getElementById('xooki-debug').style.display == 'none') {
    		xooki.html.show('xooki-debug');
    	} else {
    		xooki.html.hide('xooki-debug');
    	}
	}
}
xooki.action.evaluate = function () {
    var exp = prompt("Please enter javascript expression to evaluate");
    xooki.debugShowDetail(eval(exp));
}

// TODO, review use registration
function keyCtrl(evt) {
	var code = xooki.c.browser.NS ? evt.which : event.keyCode;
	var ctrl = xooki.c.browser.NS ? evt.ctrlKey : event.ctrlKey;
  	var key = String.fromCharCode(code);
	if (xooki.c.debug && ctrl && "d" == key) {
		xooki.action.toggleDebug();
		return false;
	}
	if (xooki.c.allowEdit && ctrl && "s" == key) {
		xooki.action.saveChanges();
		return false;
	}
	if (xooki.c.allowEdit && ctrl && "e" == key) {
		xooki.action.toggleEdit();
		return false;
	}
}

// xooki engine init function
xooki.init = function() {
    ////////////////////////////////////////////////////////////////////////////
    ////////////////// config init
    ////////////////////////////////////////////////////////////////////////////
    initConfigProperty = function(prop, value, defaultValue) {
        if (typeof this[prop] == "undefined") {
            if (typeof value == "undefined") {
                this[prop] = defaultValue;
            } else if (typeof value == "function") {
                this[prop] = value();
            } else {
                this[prop] = value;
            }
        }
    };
    if (typeof xookiConfig != "undefined") {xooki.util.mix(xookiConfig, xooki.config);}
    xooki.c.initProperty = initConfigProperty;
    xooki.c.computeRoot = function() {
    	root = xooki.pageURL;
    	// remove trailing parts of the URL to go the root depending on level
    	for (var i=0; i < xooki.c.level + 1; i++) {
    		root = root.substring(0, root.lastIndexOf('/'));
    	}
    	return root + '/';
    };
    xooki.c.computeRelativeRoot = function() {
    	return xooki.string.mul('../', xooki.c.level);
    };
    xooki.c.setImportLevel = function(level) {
        // compute roots with old level value, for paths relative to the local (non imported) root
        this.localRoot = this.computeRoot();
        this.localRelativeRoot = this.computeRelativeRoot();
        // change level and update roots
        this.level+=level;
        this.root = this.computeRoot();
        this.relativeRoot = this.computeRelativeRoot();
    };
    xooki.c.initProperty("level", 0);
    xooki.c.initProperty("root", xooki.c.computeRoot);
    xooki.c.initProperty("relativeRoot", xooki.c.computeRelativeRoot);
    xooki.c.initProperty("localRoot", xooki.c.root);
    xooki.c.initProperty("localRelativeRoot", xooki.c.relativeRoot);
    globalConfig = xooki.url.loadURL(u("config.json"), false);
    if (globalConfig != null && globalConfig.length != 0) {
        globalConfig = eval('('+globalConfig+')');
        xooki.util.mix(globalConfig, xooki.c, false);
    }
    xooki.url.evalURL(u("config.js"), false);
    xooki.url.evalURL(u("config.extra.js"), false);


    xooki.c.initProperty("defaultInputFormat", "xooki");
    xooki.c.initProperty("xookiInputFormat", ["xooki"]);
    xooki.c.initProperty("allowEdit", !batchMode && xooki.pageURL.substr(0,5) == "file:");
    
    xooki.input.format.define("xooki", ["code", "shortcuts", "xookiLinks", "jira", "includes", "printFormatImgFix", "htmltags"]);
    
    xooki.c.path = (typeof xooki.c.path != "undefined")?xooki.c.path:{};
    xooki.c.path.initProperty = initConfigProperty;
    xooki.c.path.initProperty("install", "xooki");
    xooki.c.path.initProperty("messages", xooki.p("messages.json"));
    xooki.c.path.initProperty("template", "template.html");
    xooki.c.path.initProperty("printTemplate", "printTemplate.html");
    xooki.c.path.initProperty("toc", "toc.json");
    xooki.c.path.initProperty("blankPageTpl", xooki.p("blankPageTpl.html"));
    
    
    xooki.c.css = (typeof xooki.c.css != "undefined")?xooki.c.css:{};    
        
    xooki.c.messages = xooki.json.loadURL(cu("messages")); 
	if (!batchMode) {
	    xooki.c.browser = {
	        NS: (window.Event) ? 1 : 0
	    };
    
	    // action
	    if (! xooki.c.action) xooki.c.action = 'render';
	    // TODO: better handle action extraction
		xooki.c.action = window.location.search == '?action=print'?'print':xooki.c.action;
	}
	
	var match = new RegExp("^.*\\/((?:.*\\/){"+xooki.c.level+"}[^\\/]*)(?:\\.\\w+)(?:\\?.+)?$", "g").exec(xooki.pageURL);
	if (match == null || match[1] == '') {
		xooki.c.curPageId = "index";
	} else {
		xooki.c.curPageId = match[1];
	}
    
    ////////////////////////////////////////////////////////////////////////////
    ////////////////// TOC init
    ////////////////////////////////////////////////////////////////////////////
    xooki.toc = xooki.json.loadURL(cu("toc"));
    xooki.toc.url = cu("toc");
    xooki.toc.pages = {}; // to store a by id map of pages objects
    xooki.toc.importRoot = '';
    xooki.toc.actualRoot = xooki.toc; // this is the real root of the TOC, in case of a TOC imported, it will point to the root of the TOC on which import has been performed

	// populate meta data
	(function(page, parent, index, level, prefix) {
        if (prefix.length > 0) {
            page.meta = xooki.util.mix({id: page.id}, page.meta);
            page.id = prefix + page.id;
        }
        xooki.toc.pages[page.id] = page;
        
        page.meta = xooki.util.mix({
            index: index,
            level: level,
            getSerializeValue: function(o, k) {
                if (k == 'id' && typeof this.id != 'undefined') {
                    return this.id;
                } else {
                    return o[k];
                }
            }
        }, page.meta);
        page.meta.parent = parent;
        if (typeof page.importNode != 'undefined' && !page.isImported) {
            // this node requires to import another xooki TOC
            importedTocUrl = u(page.importRoot + '/toc.json');
            importedToc = xooki.json.loadURL(importedTocUrl);
            // look for the imported node in the importedTOC and import it in main TOC
            (function(page, parent, index, level, prefix, importedToc, node, id, populateFunction) {
                if (node.id == id) {
                    xooki.util.mix(node, page, false);
                    page.id = id;
                    page.isImported = true;
                    page.meta = xooki.util.mix({
                        isTransient: function(k) {
                            // only title, importRoot and importNode should be serialized
                            return k != 'title' && k != 'importRoot' && k != 'importNode';
                        }
                    }, page.meta);
                    if (xooki.c.curPageId.indexOf(prefix) == 0) {
                        // the current page is in this imported TOC
                        xooki.toc.actualRoot = importedToc;
                        xooki.toc.url = u(page.importRoot + '/toc.json');
                        xooki.toc.importRoot = prefix;
                    }
                    populateFunction(page, parent, index, level, prefix);
                    return true;
                } else if (typeof node.children != 'undefined') {
                    for (var i=0; i<node.children.length; i++) {
                        if (arguments.callee(page, parent, index, level, prefix, importedToc, node.children[i], id, populateFunction)) {
                            return true;
                        }
                    }
                }
                return false;
            })(page, parent, index, level, page.importRoot+'/', importedToc, importedToc, page.importNode, arguments.callee);
        }
        if (typeof page.children == 'undefined') {
            page.children = [];
        } else {
            for (var i=0; i<page.children.length; i++) {
                arguments.callee(page.children[i], page, i, level+1, prefix); // recurse
            }
        }
    })(xooki.toc, null, 0, -1, '');
    
    xooki.toc.getNextPage = function(page, root) {
        if (page.children.length > 0) {
        	return page.children[0];
        } else if (page.meta.parent != null) {
        	var cur = page;
        	var next = xooki.toc.getNextSibling(cur);
        	while (next == null) {
        		cur = cur.meta.parent;
        		if (cur == null || cur == root) {
        			return null;
        		}
        		next = xooki.toc.getNextSibling(cur);
        	}
       		return next;
        } else {
        	return null;
        }
    };
    xooki.toc.getNextSibling = function(page) {
    	if (page.meta.parent == null) {
    		return null;
    	}
       	if (page.meta.parent.children.length > page.meta.index) {
       		return page.meta.parent.children[page.meta.index+1];
       	} else {
       		return null;
       	}
    };
	xooki.page = xooki.toc.pages[xooki.c.curPageId];

	if (xooki.page == null) {
		xooki.warn(t('page id not found in TOC: ${0}',xooki.c.curPageId));
		xooki.page = xooki.toc.children[0];
	} 
	if (typeof xooki.config.title == 'undefined') {
		xooki.config.title = xooki.page.title;
	}		
	xooki.config.page = xooki.page;
	
    ////////////////////////////////////////////////////////////////////////////
    ////////////////// main template loading + head output
    ////////////////////////////////////////////////////////////////////////////
	xooki.template = {};
    xooki.template.source = xooki.url.loadURL(xooki.c.action == "print"?cu("printTemplate"):cu("template"));
	if(xooki.template.source != null) {
        xooki.template.head = '';
		xooki.template.body = '${title}\n====================\n\n${body}';		
	}
	

    ////////////////////////////////////////////////////////////////////////////
    ////////////////// includes
    ////////////////////////////////////////////////////////////////////////////
    if (xooki.c.allowEdit) {
        xooki.url.include("xookiEdit.js");
    }

    for (var k in xooki.c) {
        if (typeof xooki.c[k] == "string" || typeof xooki.c[k] == "number" || typeof xooki.c[k] == "boolean") {
            xooki.debug(k+": "+xooki.c[k]);
        }
    }
};

if (batchMode) {
        try {
	    load("nashorn:mozilla_compat.js");
	} catch (e) {
	    // ignore the exception - perhaps we are running on Rhino!
	}
	importPackage(java.io);
	
	xooki.io.loadFile = function( url, warnOnError ) {
	  var str = '';
	  try {
      var r = new BufferedReader(new FileReader(url));
	  line = r.readLine();
	  while (line != null) {
		str += line + '\n';
		line = r.readLine();
	  }
	  r.close();
	  } catch (e) {
	  	if (warnOnError) {
	  		throw e;
	  	} else {
	  		xooki.debug("error occurred while loading "+url);
	  	}
	  }
	  return str;
    };
	
	xooki.io.saveFile = function (fileUrl, content) {
		p = new File(fileUrl).getParentFile();
		if (p != null) {
			p.mkdirs();
		}
		pw = new PrintWriter(new FileWriter(fileUrl));
		pw.write(content);
		pw.close();
		return true;
	}

    xooki.url.loadURL = function( url, warnOnError ) {
		return xooki.io.loadFile(url, warnOnError );
	};
	
	xooki.html.addHeader = function (head) {
		xooki.pageContent = xooki.pageContent.replace(/<\/head>/, head+'\n</head>');
	};
	
	xooki.html.setBody = function(body) {
		xooki.pageContent = xooki.pageContent.replace(/<body>(.|[^,])*<\/body>/gm, '<body>'+body+'</body>');
	}
	
	xooki.url.include = function(script_filename) {
		xooki.html.addHeader('<script language="javascript" type="text/javascript" src="'+xooki.c.relativeRoot+'xooki/'+script_filename+'"></script>');
	};
	
	xooki.input.source = function() {
		if (typeof this._source == 'undefined') {
			xooki.debug('searching source');
			var beg = xooki.pageContent.indexOf('<textarea id="xooki-source">');
			beg += '<textarea id="xooki-source">'.length;
			var end = xooki.pageContent.lastIndexOf('</textarea>');
			this._source = xooki.pageContent.substring(beg, end);
			xooki.debug('source found');
		}
		return this._source;
	}
	
	xooki.render.page = function() {
	    // realize all components available
		xooki.debug('realizing components');
	    for (var k in xooki.component) {
	        xooki.c[k] = xooki.component[k]();
	    }
	    
		xooki.debug('processing body');
		xooki.c.body = xooki.input.processed();

		xooki.debug('updating body');
		var body = xooki.string.processTemplate(xooki.template.body, xooki.c);
	    xooki.html.setBody(body);
	};

	xooki.display = function(message, background) {
		print(message);
	};
	
	xooki.debug = function (message) {
		if (xooki.c.debug) {
			print(message+'\n');
		}
	};
	var i=0;
	if (arguments.length > i && arguments[0] == '-debug') {
		xooki.c.debug = true;
		i++;
	} else {
		xooki.c.debug = false;
	}
	
	var file = 'index.html';
	if (arguments.length > i) {
		file = arguments[i];
		i++;
	}
	var generateTo = "gen";
	if (arguments.length > i) {
		generateTo = arguments[i];
		i++;
	}
	xooki.c.action = 'render';
	if (arguments.length > i) {
		xooki.c.action = arguments[i];
		i++;
	}

	xooki.pageURL = new File(file).toURL().toExternalForm();
	
	print('processing '+new File(file).getAbsolutePath()+'...\n');
	xooki.pageContent = xooki.io.loadFile(file);
    
    if (xooki.pageContent.match(/<textarea\s+id="xooki\-source">/) == null) {
        print(file + ' is not a valid xooki source. ignored.');
    } else {	
    	var m = /var\s+xookiConfig\s+=\s+{.*};/.exec(xooki.pageContent);
    	if (typeof m != 'undefined' && m != null) {
    		eval(m[0]);
    	}

        xooki.init();
        
        xooki.pageContent = xooki.pageContent.replace(/<script type="text\/javascript" src="[^"]*xooki.js"><\/script>/g, '');

    	xooki.render.main();

		var dest = generateTo.endsWith(".html") ? generateTo : generateTo+'/'+file;
		dest = dest.substring(0, dest.length-5) + ".adoc";
    	print('generating to '+dest);
        xooki.pageContent = xooki.pageContent.replace(/[\s\S]*<body>/i, '');
        xooki.pageContent = xooki.pageContent.replace(/<\/body>[\s\S]*/i, '');
    	xooki.io.saveFile(dest, xooki.pageContent);
    }
} else {
	xooki.pageURL = window.location.toString();
    xooki.init();
}

