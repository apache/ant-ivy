xooki.util.mix({debug:true, 
	jira: {ids: ['IVY'], url: 'https://issues.apache.org/jira'}, 
	shortcuts: {
		gitdir: {pre: 'https://git-wip-us.apache.org/repos/asf?p=ant-ivy.git;a=tree;f='},
		gitfile: {pre: 'https://git-wip-us.apache.org/repos/asf?p=ant-ivy.git;a=blob;f='},
		ant: {pre: xooki.c.relativeRoot+'use/', post:'.html'}
	}
}, xooki.c, false);
