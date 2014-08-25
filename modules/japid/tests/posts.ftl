<#list posts as post> 
	- title: ${post.title}
	- date: ${post.postedAt?datetime}
	- author ${post.author.name} ${post.author.gender}
</#list>