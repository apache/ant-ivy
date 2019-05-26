#	 ***************************************************************
#	 * Licensed to the Apache Software Foundation (ASF) under one
#	 * or more contributor license agreements.  See the NOTICE file
#	 * distributed with this work for additional information
#	 * regarding copyright ownership.  The ASF licenses this file
#	 * to you under the Apache License, Version 2.0 (the
#	 * "License"); you may not use this file except in compliance
#	 * with the License.  You may obtain a copy of the License at
#	 *
#	 *   https://www.apache.org/licenses/LICENSE-2.0
#	 *
#	 * Unless required by applicable law or agreed to in writing,
#	 * software distributed under the License is distributed on an
#	 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#	 * KIND, either express or implied.  See the License for the
#	 * specific language governing permissions and limitations
#	 * under the License.
#	 ***************************************************************

# Add custom functions to this module that you want to use in your Slim
# templates. Within the template you must namespace the function
# (unless someone can show me how to include them in the evaluation context).
# You can change the namespace to whatever you want.

require 'json'

module IvyDocHelpers

    class Page
        attr_accessor :id, :title, :url, :allChildIds, :children, :parent

        def initialize()
            @id = ""
            @title = ""
            @url = nil
            @allChildIds = []
            @children = []
            @parent = nil
        end

        def link(printpage)
            l = ''
            if self.url
                if self.url.start_with?('http')
                    url = self.url
                else
                    url = printpage.relativeRoot + self.url
                end
                l += '<a href="' + url + '"'
                if self.id == printpage.id
                    l += ' class="current"'
                end
                l += '>' + self.title + '</a>'
            else
                l += self.title
            end
            return l
        end

        def relativeRoot()
            p = ''
            (self.id.split("/").length-1).times do |e|
                p += '../'
            end
            return p
        end

        def breadCrumb()
            b = '<span class="breadCrumb">'
            b += breadCrumbStep(self)
            b += '</span>'
            return b
        end

        def breadCrumbStep(page)
            b = ' '
            if page.parent && page.parent.parent
                b += breadCrumbStep(page.parent)
                b += ' &gt; '
            end
            b += page.link(page)
            return b
        end

        def rootpage()
            if self.parent
                return self.parent.rootpage()
            else
                return self
            end
        end

        def menu()
            return innermenu(rootpage())
        end

        def innermenu(page)
            m = '<ul id="treemenu" class="treeview">' + "\n"
            page.children.each do |p|
                m += '<li id="xooki-' + (p.id || "undefined") + '"'
                if p.children.length > 0
                    m += ' class="submenu"'
                end
                m += '>'
                m += p.link(self)
                if p.children.length > 0
                    m += '<ul class="'
                    if p.allChildIds.include? self.id
                        m += 'open'
                    else
                        m += 'closed'
                    end
                    m += '">'
                    m += innermenu(p)
                    m += '</ul>'
                end
                m += "</li>\n"
            end
            m += "</ul>\n"
            return m
        end

    end

    def self.page(basedir, docfile, version)
        rootpage = loadPages(basedir, version)
        pageId = docfile[basedir.length+1..docfile.rindex(/\./)-1]
        p = findPage(rootpage, pageId)
        if !p
            p = Page.new
        end
        return p
    end

    def self.loadPages(basedir, version)
        rootpage = Page.new
        toc = JSON.parse(IO.read(basedir + "/toc.json"))
        toc['children'].each do |child|
            rootpage.children << loadPage(basedir, rootpage, child, "", version)
        end
        return rootpage
    end

    def self.loadPage(basedir, parent, node, path, version)
        p = Page.new
        p.title = node['title']
        p.title.sub! '${version}', version
        p.parent = parent
        if node.has_key?("importRoot")
            p.id = path + node['importRoot'] + '/' + node['importNode']
            p.url = p.id + ".html"
            toc = JSON.parse(IO.read(basedir + '/' + node['importRoot'] + "/toc.json"))
            toc['children'].each do |child|
                p.children << loadPage(basedir, node, child, path + node['importRoot'] + '/', version)
            end
        else
            p.id = node['id']
            if !(node.has_key?("isAbstract"))
                if node.has_key?("url")
                    p.url = node['url']
                else
                    p.url = path + node['id'] + ".html"
                end
            end
            if node.has_key?("children")
                node['children'].each do |child|
                    p.children << loadPage(basedir, p, child, path, version)
                end
            end
        end
        p.children.each { |cp| p.allChildIds += cp.allChildIds }
        if p.id
            p.allChildIds << p.id
        end
        return p
    end

    def self.findPage(parent, pageId)
        parent.children.each do |p|
            if p.id == pageId
                return p
            end
            if p.children.length > 0
                found = findPage(p, pageId)
                if found
                    return found
                end
            end
        end
        return nil
    end

end
