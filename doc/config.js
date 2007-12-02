xooki.util.mix({debug:true, 
	jira: {ids: ['IVY'], url: 'http://issues.apache.org/jira'}, 
	shortcuts: {
		svn: {pre: 'https://svn.apache.org/repos/asf/ant/ivy/core/trunk/'},
		ant: {pre: xooki.c.relativeRoot+'use/', post:'.html'}
	}
}, xooki.c, false);
