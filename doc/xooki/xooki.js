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
var xooki = {};
xooki.console = ""; // used for debugging purpose only, and only when the debug div is not yet created

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
  return xooki.c.root + path;
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
                
        loadURL: function( url ) {
        	req = this.newXmlHttpRequest();
        	if(req) {
        		try {
        			req.open("GET", url, false);
        			req.send("");
        	
        			return req.responseText;
        		} catch (e) {
        			xooki.error(e, t("problem while loading URL ${0}", url));
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
				           if (req.status == 200) {
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
            document.write(' src="' + script_filename + '">');
            document.write('</' + 'script' + '>');
        },

        action: function(action) {        
            // returns the url for an action on the same page
            loc = window.location.toString();
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
        return this.findSection(str, new RegExp('<'+element+'(\\s*\\w+="[\\w\\s]*")*>'), new RegExp('</'+element+'>'), from);
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
            return null;
        }
        var closeResult = this.find(str, close, from);        
        if (closeResult == null || closeResult.begin < openResult.end) {
            return null;
        }
        if (openResult.end <= openResult.begin || closeResult.end <= closeResult.begin) {
            // empty match are not allowed
            return null;
        }
        
        var children = [];
        var child = this.findSection(str, open, close, openResult.end);
        while (child != null) {
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

        return {
            outerStart: openResult.begin,
            innerStart: openResult.end,
            innerEnd: closeResult.begin,
            outerEnd: closeResult.end,
            children: children
        };        
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
    var console = document.getElementById('xooki-console');
    if (console) {
        console.value += message + "\n";
    } else {
        xooki.console += message + "\n";
    }
}

xooki.debugShowDetail = function (message) {
    var detail = document.getElementById('xooki-debug-detail');
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
    } 
};

xooki.component = {
    childrenList: function () {
    	if (xooki.page.children.length > 0) {
    		childrenList = '<ul class="'+css('childrenList')+'">';
    		for (var i in xooki.page.children) {
    			childrenList+='<li><a href="'+pu(xooki.page.children[i].id)+'">'+xooki.page.children[i].title+'</a></li>';
    		}
    		childrenList += "</ul>";
    		return childrenList;
    	} else {
    	   return "";
        }
    },
    
    menu: function () {
    	var menu = '<ul id="'+css("treemenu")+'" class="treeview">';
    	menu += (function (page) {
        	var menu = '';
        	for (var i  in page.children) {
        		menu += '<li id="xooki-'+page.children[i].id+'"><a href="'+pu(page.children[i].id)+'" '+(page.children[i].id == xooki.page.id?'class="current"':'')+'>'+page.children[i].title+'</a>';
        		smenu = arguments.callee(page.children[i]);
        		if (smenu != '') {
        			menu += '<ul ';
        			if (smenu.indexOf('id="xooki-'+xooki.page.id+'"') != -1 
        				|| page.children[i].id == xooki.page.id) {
        				// either a descendant or the node processed is the current page node
        				// we specify that the menu must be opened by default
        				menu += 'rel="open"';
        			}
        			menu += '>'+smenu+'</ul>';
        		} 
        		menu += '</li>';
        	}
        	return menu;
        })(xooki.toc);
    	menu += '</ul>';
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
    printerFriendly += xooki.input.format.main(source) + "<hr/>";
    // inject block in page
    var pf = document.getElementById('xooki-printerFriendly');
    pf.innerHTML += printerFriendly;    
    
    // continue recursive loading
   	var nextPage = xooki.toc.getNextPage(page, root);
   	if (nextPage != null) {
    	xooki.url.asyncLoadURL(pu(nextPage.id), xooki.render.printerFriendlyAsyncLoader, [root, nextPage]);
   	}
};
xooki.render.printerFriendly = function() {
    for (var k in xooki.component) {
        xooki.c[k] = xooki.component[k]();
    }
    
	xooki.c.body = xooki.c.messages
	+ "<div id='xooki-printerFriendly'></div>" // div where printer friendly content will be put
    + xooki.c.debugPanel;
    
    document.body.innerHTML = xooki.string.processTemplate(xooki.template.body, xooki.c);
    
    // start async loading of content
    xooki.url.asyncLoadURL(pu(xooki.page.id), xooki.render.printerFriendlyAsyncLoader, [xooki.page, xooki.page]);
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

    document.body.innerHTML = xooki.string.processTemplate(xooki.template.body, xooki.c);
    
    xooki.input.applyChanges();
    
    // enable dynamic tree menu 
	ddtreemenu.createTree(css("treemenu"), false);
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
        if (document.getElementById('xooki-source') != null) {
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
        main: function(source) {
            // formats an input source
            if (xooki.c.inputFormat && typeof this.getInputFilters(xooki.c.inputFormat) != "undefined") {
                format = xooki.c.inputFormat;
            } else {
                format = xooki.c.defaultInputFormat;
            }
            filters = this.getInputFilters(format);
            for (var i in filters) {
                f = xooki.input.filters[filters[i]];
                if (typeof f == "function") {
                	try {
                    	source = f(source); // process filter
                    } catch (e) {
	                    xooki.error(e, t("error occured while processing filter ${0}", filters[i]));
                    }
                } else {
                    xooki.error(t("unknown filter ${0} used in input format ${1}", filters[i], format));
                }
            }
            return source;
        }
    }, 
    
    filters: {
        url: function (input) {
            // handle urls
            return input.replace(new RegExp("(?:file|http|https|mailto|ftp):[^\\s'\"]+(?:/|\\b)", "g"), function (str, offset, s) {
                var before = s.substring(0,offset);
                if (before.match(/(href|src)="$/)) {
                    return str;
                } else {
                    return '<a href="'+str+'">'+str+'</a>';
                }
            });
        },
        
        shortcuts: function (input) {
            // handle shortcut links like this:
            //    [[svn:build.xml]] => <a href="https://xooki.svn.sourceforge.net/svnroot/xooki/trunk/build.xml">build.xml</a>
            //    [[svn:test/example.js a good example]] => <a href="https://xooki.svn.sourceforge.net/svnroot/xooki/trunk/test/example.js">a good example</a>
            // needs to be configured in xooki config like this
            //      xooki.c.shortcuts.<any shortcut>.url = base url of the shortcut. 
            //      ex: xooki.c.shortcuts.svn.url = https://xooki.svn.sourceforge.net/svnroot/xooki/trunk/
            return input.replace(new RegExp("\\[\\[([^:]+):([^\\]]+)\\]\\]", "g"), function (str, prefix, code, offset, s) {
            	if (typeof xooki.c.shortcuts[prefix] == "undefined") {
            		return str;
            	}
                var index = code.indexOf(' ');
                var path = index>0?code.substring(0,index):code;
                
                var title = index>0?code.substring(index+1):path;
                var pre = typeof xooki.c.shortcuts[prefix].pre == "undefined"?'':xooki.c.shortcuts[prefix].pre;
                var post = typeof xooki.c.shortcuts[prefix].post == "undefined"?'':xooki.c.shortcuts[prefix].post;
                return '<a href="'+pre+path+post+'">'+title+'</a>';
            });
        },
        
        xookiLinks: function (input) {
            // handle xooki links like this:
            //    [[page/id]]
            //    [[page/id My Title]]
            return input.replace(new RegExp("\\[\\[([^\\]]+)\\]\\]", "g"), function (str, code, offset, s) {
                var index = code.indexOf(' ');
                var id = index>0?code.substring(0,index):code;
                
                var title;
                var url = pu(id);
                if (index>0) {
                	title = code.substring(index+1);
               	} else if (typeof xooki.toc.pages[id] != "undefined") {
               	    title = xooki.toc.pages[id].title;
               	} else {
               		title = code;
               		url = u(code);
               	}
                return '<a href="'+url+'">'+title+'</a>';
            });
        },
        
        wikiMarkup: function (input) { 
            // handle bold
            input = input.replace(new RegExp("\\*([^\n]+)\\*", "g"), "<b>$1</b>");
            
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
            input = input.replace(new RegExp("(("+xooki.c.jira.ids.join("|")+")-\\d+)([^\"\\d])", "g"), '<a href="'+xooki.c.jira.url+'/browse/$1">$1</a>$3');
            
            return input;
        },
        
        code: function (input) {
            codeSection = xooki.string.findXmlSection(input, "code");
            from = 0;
            while (codeSection != null) {
                processedSection = "<pre>" 
                    + input.substring(codeSection.innerStart, codeSection.innerEnd).replace(/</g, "&lt;").replace(/>/g, "&gt;") // .replace(/\n/g, "<br/>")
                    + "</pre>";
                input = input.substring(0, codeSection.outerStart)
                    + processedSection
                    + input.substring(codeSection.outerEnd);
                from = codeSection.outerStart + processedSection.length;
    
                codeSection = xooki.string.findXmlSection(input, "code", from);
            }
            return input;
        },
        
        lineBreak: function (input) {
            return input.replace(new RegExp("\r?\n", "g"), function (str, offset, s) {
                var before = s.substring(0,offset);
                var after = s.substring(offset+str.length);
                if (after.match(/^<\/?(ul|table|li|pre|div)(\s*\w+="[^"]+")*\s*>/i) || (before.match(/<\/?\w+(\s*\w+="[^"]+")*\s*\/?>\s*$/i) && !before.match(/<\/?(a|b|strong|em|i|big|br class="xooki-br")(\s*\w+="[^"]+")*\s*\/?>\s*$/i))) { 
                    return '\n';
                } else {
                    return '<br class="xooki-br"/>'; // the class is not really necessary but allow to distinguish generated br from input one
                }
            });
        }
    },
    
    
    applyChanges: function() {
    	document.getElementById('xooki-body').innerHTML = xooki.input.processed();
    }
};


xooki.postProcess = function() {
	xooki.render.main();
	window.onkeypress = keyCtrl;
};

// init xooki engine
(function() {
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
    xooki.config = {};
    xooki.c = xooki.config;
    if (typeof xookiConfig != "undefined") {xooki.util.mix(xookiConfig, xooki.config);}
    xooki.c.initProperty = initConfigProperty;
    xooki.c.initProperty("level", 0);
    xooki.c.initProperty("root", function() {
    	root = window.location.toString();
    	// remove trailing parts of the URL to go the root depending on level
    	for (var i=0; i < xooki.c.level + 1; i++) {
    		root = root.substring(0, root.lastIndexOf('/'));
    	}
    	return root + '/';
    });

    var globalConfig = xooki.json.loadURL(u("config.json"));
    if (globalConfig != null) {
        xooki.util.mix(globalConfig, xooki.config, false);
    }

    xooki.c.initProperty("defaultInputFormat", "xooki");
    xooki.c.initProperty("xookiInputFormat", ["xooki"]);
    xooki.c.initProperty("allowEdit", document.location.toString().substr(0,5) == "file:");
    
    xooki.input.format.define("xooki", ["code", "shortcuts", "url", "xookiLinks", "jira", "lineBreak"]);
    
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
    xooki.c.browser = {
        NS: (window.Event) ? 1 : 0
    };
    
    // action
    // TODO: better handle action extraction
    xooki.c.action = window.location.search == '?action=print'?'print':xooki.c.action;
    
    ////////////////////////////////////////////////////////////////////////////
    ////////////////// TOC init
    ////////////////////////////////////////////////////////////////////////////
    xooki.toc = xooki.json.loadURL(cu("toc"));
    xooki.toc.pages = {}; // to store a by id map of pages objects

	// populate meta data
	(function(page, parent, index, level) {
        xooki.toc.pages[page.id] = page;
        
        page.meta = {
            parent: parent,
            index: index,
            level: level
        };
        if (typeof page.children == 'undefined') {
            page.children = [];
        } else {
            for (var i=0; i<page.children.length; i++) {
                arguments.callee(page.children[i], page, i, level+1); // recurse
            }
        }
    })(xooki.toc, null, 0, -1);
    
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
	
	var match = new RegExp("^.*\\/((?:.*\\/){"+xooki.config.level+"}[^\\/]*)(?:\\.\\w+)(?:\\?.+)?$", "g").exec(window.location.toString());
	var curPageId;
	if (match == null || match[1] == '') {
		curPageId = "index";
	} else {
		curPageId = match[1];
	}
	xooki.page = xooki.toc.pages[curPageId];

	if (xooki.page == null) {
		xooki.warn(t('page id not found in TOC: ${0}',curPageId));
	} else {
		if (typeof xooki.config.title == 'undefined') {
			xooki.config.title = xooki.page.title;
		}		
	}
	xooki.config.page = xooki.page;
	
    ////////////////////////////////////////////////////////////////////////////
    ////////////////// main template loading + head output
    ////////////////////////////////////////////////////////////////////////////
	xooki.template = {};
    xooki.template.source = xooki.url.loadURL(xooki.c.action == "print"?cu("printTemplate"):cu("template"));
	if(xooki.template.source != null) {
		xooki.template.head = xooki.template.source.match(/<head>([^§]*)<\/head>/im)[1];
		
        var head = xooki.string.processTemplate(xooki.template.head, xooki.config);
		head = head.replace(/href="([^\\$:"]+)"/g, 'href="'+xooki.c.root+'$1"');
		document.write(head);

		var body = xooki.template.source.match(/<body>([^§]*)<\/body>/im)[1];
		body = body.replace(/href="([^\\$:"]+)"/g, 'href="'+xooki.c.root+'$1"');
		xooki.template.body = body.replace(/src="([^\\$:"]+)"/g, 'src="'+xooki.c.root+'$1"');		
	}
	

    ////////////////////////////////////////////////////////////////////////////
    ////////////////// includes
    ////////////////////////////////////////////////////////////////////////////
    xooki.url.include(xooki.u("tree/simpletreemenu.js"));
    if (xooki.c.useTrimPath) {
        xooki.url.include(xooki.u("trimpath/template.js"));
    }
    if (xooki.c.allowEdit) {
        xooki.url.include(xooki.u("xookiEdit.js"));
    }

    for (var k in xooki.c) {
        if (typeof xooki.c[k] == "string" || typeof xooki.c[k] == "number" || typeof xooki.c[k] == "boolean") {
            xooki.debug(k+": "+xooki.c[k]);
        }
    }
})();

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
