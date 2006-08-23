# 
# Generates HTML documentation from DrupalXML (DXML) format
# 
# This script has been written for Ivy usage, and may contain things specific to Ivy
# 
# The performances are also very poor, I don't know if it's ruby or my fault (certainly mine, I'm still
# a ruby newbie), but since this generation doesn't occur too often for Ivy, this is fine
# 
# Note also that I had to modify the export_dxml module of drupal to put the node path in the xml
# file (since we use heavily the url alias feature of drupal).
# 
# This modification is as simple as adding the following lines in 
# book_node_visitor_dxml_pre - $Id: export_dxml.module,v 1.3 2005/11/29 19:28:10 puregin Exp $:
#    // check if we have an alias
#    $path = "node/$node->nid";
#
#    $result = db_query("SELECT dst FROM {url_alias} WHERE src = '%s'", $path);
#    if (db_num_rows($result)) {
#        $node->path = db_result($result);
#    } else {
#        $node->path = $path;
#    }
#    $output .= " path='" . $node->path . "'";
#
# Note that this only work if the path module is activated (due to the use of the url_alias table
# 


require 'FileUtils'
require 'rexml/document'
include REXML

def p o 
  now = Time.now
  $stdout.puts(now.strftime("%H:%M:%S") + ":" + ("%3d" % (now.usec / 1000)) + " | " + o)
  $stdout.flush
end

class DxmlToHtml

  ##
  # initialise a dxml to html converter
  # 
  # site is the web site on which relative links not part of the book should be redirected, with no trailing slash
  # template if the template file to use, look template.html for an example (tokens are of the form #{token}
  # ext is the extension to append to the generated files and links
  def initialize(site, template, ext = "html")
    @site = site
    @template = template
    @ext = ext
  end

private
  def process_template(tpl, fields, dest) 
    ptpl = tpl.dup
    ptpl.gsub!(/#\{([^\}]+)\}/) {|s| fields[$1]}
    f = File.new(dest, 'w')
    f.puts ptpl
    f.close
  end
  
  def gen_navigation(node)
    content = ''
    buf = content
    prevnode = nil
    pathdepth = node.elements["nodeinfo"].attributes['path'].count('/')
    begin
      buf << '<ul class="menu">'
      node.each_element("./node") do |child| 
        isprev = child == prevnode
        if child.elements["./node"]
          if (isprev)
            clss = 'expanded'
          else
            clss = 'collapsed'
          end
        else
          clss = 'leaf'
        end
        buf << '<li class="' << clss << '"><a href="' << ("../" * pathdepth << child.elements["nodeinfo"].attributes['path']+"."+@ext)  << '">' << child.elements["title"].text << '</a>'
        if (isprev)
          content = buf << content # put pre buffer at the beginning of the previous content
          buf = content # now append after the content
        end
        buf << "</li>"
      end
      buf << "</ul>"
    
      buf = '' # now fill a pre buffer
      prevnode = node
      node = node.parent
    end while node.parent
    
    return content
  end

#  def gen_navigation(node, path = nil, pathdepth = node.elements["nodeinfo"].attributes['path'].count('/'),     content = '')
#    if (!path) 
#      path = Array.new
#      while node.parent
#        path.push node
#        node = node.parent
#      end
#    end
#    
#    content << '<ul class="menu">'
#    node = path.pop
#    node.each_element("./node") do |child| 
#      if child.elements["./node"]
#        if (child == path.last)
#          clss = 'expanded'
#        else
#          clss = 'collapsed'
#        end
#      else
#        clss = 'leaf'
#      end
#      content << '<li class="' << clss << '"><a href="' << ("../" * pathdepth << child.elements["nodeinfo"].attributes['path']+"."+@ext)  << '">' << child.elements["title"].text << '</a>'
#      if (child == path.last)
#        gen_navigation(node,path,pathdepth,content)
#      end
#      content << "</li>"
#    end
#    content << "</ul>"
#    return content
#  end

public  
  ##
  # generates html files from a DXML file into the speficied directory
  # Note that if files already exist in the target directory they will be overwritten!
  # 
  # A file will be created for each node in the DXML file, the name of the file being the path 
  # of the node + .ext (where ext is configured in the object instanciation).
  # 
  # All hyperlinks are processed so that any link to another page of the book will be automatically
  # converted to a relative link in the generated files. 
  # 
  # Other site relative links are prefixed with the site configured in the object instanciation,
  # external absolute links are kept as is.
  # 
  # Links processing only works with absolute path on the site (/ivy/doc/conf, for instance), and not 
  # with relative path, or full absolute path (like http://www.jayasoft.org/ivy).
  # 
  def gen(book, todir)    
    p "reading template..."
    tpl = IO.readlines(@template).join()
    
    p "parsing dxml (#{book})..."
    doc = Document.new(File.new(book))
    root = doc.root
    
    p "building node index..."
    nodes = Hash.new
    root.each_element("//node") do |node|
    nodes[node.elements["nodeinfo"].attributes['path']] = node
    end
    
    p "starting generation..."
    root.each_element("//node") do |node|   
      path = node.elements["nodeinfo"].attributes['path']
      pathdepth = path.count("/")
      node_file_path = todir+"/"+path+"."+@ext
      p "generating #{node_file_path}"
    
      FileUtils.mkdir_p(File.dirname(node_file_path))
      
      
      title = node.elements["title"].text
      
      base = "../" * pathdepth
      
      content = node.elements["content"].cdatas.to_s
      content.gsub!(/href="\/([^"]+)"/) do |s|
        if nodes[$1]
          'href="'+base+$1 +'.'+@ext+'"' 
        else
          'href="'+@site + '/' +$1+'"'
        end
      end
      
#      p "  generating navigation menu"
      navigation = gen_navigation(node)
      
#      p "  processing template"
      process_template(tpl, {
        'path' => path, 
        'base' => base, 
        'title' => title, 
        'navigation' => navigation, 
        'content' => content
        }, node_file_path) 
#      p "  end"
    end
  end

end

p "starting generator"
g = DxmlToHtml.new("http://www.jayasoft.org", "src/doc/template.html")
g.gen("src/doc/ivy-book.xml", "doc")
