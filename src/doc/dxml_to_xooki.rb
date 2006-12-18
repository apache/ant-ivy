# 
# Generates Xooki documentation from DrupalXML (DXML) format
# Used to migrate doc to ASF incubator
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
  
  def get_path(node)
    return node.elements["nodeinfo"].attributes['path'].gsub(/^ivy\//, '')
  end
  
  def gen_toc(node)
    content = '{'
    content << '"id":"' << get_path(node) << '",'
    content << '"title":"' << node.elements["title"].text << '",'
    content << '"children":['
    node.each_element("./node") do |child| 
      content << gen_toc(child) << ','
    end
    content << ']'
    content << '}'
    return content
  end


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
      nodes[get_path(node)] = node
    end
    
    p "generating toc..."
    FileUtils.mkdir_p(todir)
    f = File.new(todir+"/toc.json", 'w')
    f.puts gen_toc(root)
    f.close    
    
    p "starting generation..."
    root.each_element("//node") do |node|   
      path = get_path node
      pathdepth = path.count("/")
      node_file_path = todir+"/"+path+"."+@ext
      p "generating #{node_file_path}"
    
      FileUtils.mkdir_p(File.dirname(node_file_path))
      
      
      title = node.elements["title"].text
      
      base = "../" * pathdepth
      
      content = node.elements["content"].cdatas.to_s
      content.gsub!(/href="\.?\/([^"]+)"/) do |s|
        npath = $1.gsub(/^ivy\//, '')
        if nodes[npath]
          'href="'+base+npath +'.'+@ext+'"' 
        else
          'href="'+@site + '/' +npath+'.'+@ext+'"'
        end
      end
            
#      p "  processing template"
      process_template(tpl, {
        'level' => pathdepth, 
        'relroot' => base, 
        'body' => content
        }, node_file_path) 
#      p "  end"
    end
  end

end

p "starting generator"
g = DxmlToHtml.new("http://incubator.apache.org/ivy", "src/doc/blankPageTpl.html")
g.gen("src/doc/ivy-raw-book.xml", "src/doc/xooki")
