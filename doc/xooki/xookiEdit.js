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

// this file is included only in edit mode

xooki.url.include(xooki.u("tiddly/util.js"));

if (typeof xooki.io == "undefined") {
    xooki.io = {};
}

xooki.io.removeFile = function (filePath) {
	var r = null;
	if((r == null) || (r == false))
		r = xooki.io.mozillaRemoveFile(filePath);
	if((r == null) || (r == false))
		r = xooki.io.ieRemoveFile(filePath);
	return(r);
}

xooki.io.mozillaRemoveFile = function(filePath) {
	if(window.Components)
		try
			{
			netscape.security.PrivilegeManager.enablePrivilege("UniversalXPConnect");
			var file = Components.classes["@mozilla.org/file/local;1"].createInstance(Components.interfaces.nsILocalFile);
			file.initWithPath(filePath);
			if (!file.exists())
				return null;
			file.remove(false);
			return(true);
			}
		catch(e)
			{
			return(false);
			}
	return(null);
}

xooki.io.ieRemoveFile = function(filePath) {
	try
		{
		var fso = new ActiveXObject("Scripting.FileSystemObject");
		}
	catch(e)
		{
		//alert("Exception while attempting to save\n\n" + e.toString());
		return(null);
		}
	fso.DeleteFile(filePath, false);
	return(true);
}


if (typeof xooki.string == "undefined") {
    xooki.string = {};
}

xooki.string.escapeString = function(str) {
// borrowed from dojo
//summary:
//	Adds escape sequences for non-visual characters, double quote and backslash
//	and surrounds with double quotes to form a valid string literal.
	return ('"' + str.replace(/(["\\])/g, '\\$1') + '"'
		).replace(/[\f]/g, "\\f"
		).replace(/[\b]/g, "\\b"
		).replace(/[\n]/g, "\\n"
		).replace(/[\t]/g, "\\t"
		).replace(/[\r]/g, "\\r"); // string
};

if (typeof xooki.json == "undefined") {
    xooki.json = {};
}

xooki.json.serialize = function (o, indent) {
        // borrowed and adapted from dojo
    	// summary:
    	//		Create a JSON serialization of the object.
    	// return:
    	//		a String representing the serialized version of the passed object
    	if (!indent) {
    	   indent = "";
        }
		var objtype = typeof(o);
		if(objtype == "undefined"){
			return "undefined";
		}else if((objtype == "number")||(objtype == "boolean")){
			return o + "";
		}else if(o === null){
			return "null";
		}
		if (objtype == "string") { return xooki.string.escapeString(o); }
		if(objtype == "function"){
		    // do not encode functions
			return null;
		}
		// recurse
		var me = arguments.callee;
		// short-circuit for objects that support "json" serialization
		// if they return "self" then just pass-through...
		var newObj;
		// array
		if(objtype != "function" && typeof(o.length) == "number"){
			var res = [];
			for(var i = 0; i < o.length; i++){
				var val = me(o[i], indent+"  ");
				
				if(typeof(val) != "string"){
					val = "undefined";
				}
				res.push(val);
			}
			return " [\n" + res.join(",\n") + "\n"+indent+"]\n";
		}
		// generic object code path
		res = [];
		for (var k in o){
		    if ("meta" == k) {
		      continue;
            }
			var useKey;
			if (typeof(k) == "number"){
				useKey = '"' + k + '"';
			}else if (typeof(k) == "string"){
				useKey = xooki.string.escapeString(k);
			}else{
				// skip non-string or number keys
				continue;
			}
			val = me(o[k], indent+"    ");
			if(typeof(val) != "string"){
				// skip non-serializable values
				continue;
			}
			res.push(indent+"  "+useKey + ":" + val);
		}
		return indent+"{\n" + res.join(",\n") + indent+"}";
};

xooki.component.toolbar = function () {
	return	'<div style="position:absolute;top:2px;right:30px;" id="xooki-toolbar">'
				+ '<a href="javascript:xooki.action.toggleEdit()"><img src="'+xooki.u('images/edit.gif')+'" title="toggle edit'+(xooki.c.browser.NS?' (CTRL+E)':'')+'"/></a>'
				+ '<a href="javascript:xooki.action.saveChanges()"><img src="'+xooki.u('images/save.gif')+'" title="save'+(xooki.c.browser.NS?' (CTRL+S)':'')+'"/></a>'
				+ '<a href="javascript:xooki.action.remove()"><img src="'+xooki.u('images/delete.gif')+'" title="remove"/></a>'
				+ '<a href="javascript:xooki.action.createChild()"><img src="'+xooki.u('images/addchild.gif')+'" title="create child"/></a>'
				+ '<a href="javascript:xooki.action.movePageUp()"><img src="'+xooki.u('images/up.gif')+'" title="move page up in TOC"/></a>'
				+ '<a href="javascript:xooki.action.movePageDown()"><img src="'+xooki.u('images/down.gif')+'" title="move page down in TOC"/></a>'
				+ (xooki.c.debug?'<a href="javascript:xooki.action.toggleDebug()"><img src="'+xooki.u('images/debug.gif')+'" title="toggle xooki debug mode"/></a>':'')
			+ '</div>';
};
    
xooki.component.editZone = function () {
    return '<div id="xooki-edit" style="display:none">'
        + '<table border="0"><tr><td valign="top">Title</td><td><input id="xooki-input-title" type="text" value="'+xooki.page.title+'"></input></td></tr>'
        + '<tr><td valign="top">Content</td><td><textarea rows="20" cols="80" id="xooki-source">'+xooki.input.source()+'</textarea></td></tr>'
        + '<tr><td colspan="2" align="right"><input type="button" value="Save" onclick="javascript:xooki.action.saveChanges()"/> <input type="button" value="Preview" onclick="javascript:xooki.action.previewChanges()"/> <input type="button" value="Discard" onclick="javascript:xooki.action.discardChanges()"/></td></tr></table></div>';
}

xooki.url.reload = function() {
    window.location = window.location;
}



xooki.action.quitEdit = function () {
    xooki.input.applyChanges();
	xooki.html.hide('xooki-edit');
	xooki.html.show('xooki-content');
}
xooki.action.edit = function () {
	xooki.html.hide('xooki-content');
	xooki.html.show('xooki-edit');
}
xooki.action.toggleEdit = function () {
	if (document.getElementById('xooki-edit').style.display == 'none') {
		xooki.action.edit();
	} else {
		xooki.action.quitEdit();
	}
}
xooki.action.remove = function () {
    if (confirm(t("The current page will be removed from the table of content, and deleted on file system.\nNote that all children will be removed from the table of content too, but not from the file system!\nAre you sure you want to delete the current page?"))) {

        // the page to which we'll be redirected...
        var nextPage;
        var index = xooki.page.meta.index;
        var parent = xooki.page.meta.parent;
        if (index > 0) {
            // ... will be the previous sibling if there is one ...
            nextPage = parent.children[index - 1];
        } else if (parent.children.length > 1) {
            // ... or the next sibling if there is one ...
            nextPage = parent.children[index + 1];        
        } else if (parent != xooki.toc) {
            // ... or the parent if the parent is not the toc root ...
            nextPage = parent;        
        } else {
            // ... otherwise it s a problem
            xooki.error(t("Cannot delete the sole page!"));
            return;
        }
        
        parent.children.splice(index, 1); // remove node from toc
        xooki.toc.save();
        
        xooki.io.removeFile(xooki.io.getLocalPath(window.location.toString()));
        
        window.location = pu(nextPage.id);
    }
}
xooki.action.discardChanges = function () {
    xooki.url.reload();
}
xooki.action.previewChanges = function () {
    xooki.action.quitEdit();
}
xooki.action.saveChanges = function () {
	var originalPath = document.location.toString();
	var localPath = xooki.io.getLocalPath(originalPath);
	
	// Load the original file
	var original = xooki.io.loadFile(localPath);
	if(original == null) {
		xooki.error(t("Impossible to load original file: ${0}", localPath));
		return;
	}
	
	var startSaveArea = '<textarea id="xooki-source">';
	var posOpeningArea = original.indexOf(startSaveArea);
	var posClosingArea = original.indexOf('</textarea>');
	
	xooki.page.title = document.getElementById('xooki-input-title').value;
	xooki.toc.save();
	
	var save;
	try {
		// Save new file
		var revised = original.substr(0,posOpeningArea + startSaveArea.length) + "\n" +
					xooki.input.source() +
					original.substr(posClosingArea);
					
		save = xooki.io.saveFile(localPath,revised);
	} catch (e) {
		xooki.error(e);
	} 
    if(save) {
		xooki.info(t("saved to ${0}",localPath));
		
		// TODO: see if we are able to apply title change without reloading
		setTimeout(function() {xooki.url.reload();}, 800);
	} else
		xooki.error(t("Impossible to save changes to ${0}", localPath));
}


xooki.action.movePageUp = function () {
    xooki.action.movePage(-1);
}
xooki.action.movePageDown = function () {
    xooki.action.movePage(1);
}

xooki.action.movePage = function (delta) {
    var index = xooki.page.meta.index;
    var parent = xooki.page.meta.parent;

    // check if node can move
    if (index + delta < 0) {
        xooki.info(t("Can't move first page up"));
        return;
    }
    if (index + delta >= parent.children.length) {
        xooki.info(t("Can't move last page down"));
        return;
    }

    // move node in toc    
    parent.children.splice(index, 1);
    parent.children.splice(index+delta, 0, xooki.page);
    
    xooki.toc.save();
    xooki.url.reload();
}

xooki.action.createChild = function () {
    titleToId = function (title) {
        return title.replace(/\s+/g, '');
    }

    var title = prompt("Child page title?", "");
    var id = prompt("Child page path?", titleToId(title));
    
    xooki.action.createChildPage({"id": id, "title": title, "children": []});
}

xooki.action.createChildPage = function (child) {
    if (typeof child.level == 'undefined') {
        child.level = child.id.split('/').length - 1;
    }
        
    var pagetpl = xooki.url.loadURL(cu("blankPageTpl"));
    if (pagetpl != null) {
        var childURL = pu(child.id);
        var localPath = xooki.io.getLocalPath(childURL);
        var original = xooki.io.loadFile(localPath);
        if (original != null) {
            if (!window.confirm(t("File for child page:\n${0}\nalready exists.\nAre you sure you want to continue and overwrite it?", localPath))) 
                return;
        }
        
        xooki.page.children.push(child);
        xooki.toc.save();
        
        // usually used by templates
        if (typeof child.relroot == 'undefined') {
            child.relroot = "";
        	for (var i=0; i < child.level; i++) {
        		child.relroot += "../";
        	}
        }
		var revised = xooki.string.processTemplate(pagetpl, child);
    	var save;
    	try {
    		// Save new file
    		save = xooki.io.saveFile(localPath,revised);
    	} catch (e) {
    		xooki.error(e);
    	} 
        if(save) {
    		// go to child page
            window.location = childURL;
    	} else
    		xooki.error(t("Impossible to save changes to ${0}", localPath));
    }
}


xooki.toc.save = function (revised) {
    if (!revised) {
        revised = xooki.json.serialize({children: this.children});
    }
	var save;
	var tocPath = xooki.io.getLocalPath(cu("toc"));
	try {
		save = xooki.io.saveFile(tocPath, revised);
	} catch (e) {
		xooki.error(e);
	} 
    if(!save) 
		xooki.error(t("Impossible to save changes to ${0}", tocPath));
}


xooki.util = {}
xooki.util.copy = function(o) {
    var copy = {};
    for (var k in o) {
        copy[k] = o[k];
    }   
    return copy;
}
