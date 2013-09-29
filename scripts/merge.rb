class String
  def end_index(s)
    index(s) + s.size()
  end
  def end_rindex(s)
    rindex(s) + s.size()
  end
end

def extract_content_from(file_name)
  html = File.read(file_name)

  from = html.index('<style')
  to = html.end_index('</style>')
  style = html[from..to]

  from = html.rindex('<script>')
  to = html.end_rindex('</script>')
  script = html[from..to]

  [style, script]
end

def remove_margin_style(html)
  html.gsub!(/margin:.*?;/, '')
  html
end

def remove_header_span(html)
  html.gsub!(/var header.*?;/m, '')
  html.gsub!(/headerSpan\..*?;/m, '')
  html.gsub!(/header\..*?;/m, '')
  html
end

def reduce_width(html)
  html.gsub!(/var width =.*?;/, 'var width = 800;')
  html.gsub!(/width =.*?,/, 'width = 740,') # this is specific for change size chart
  html.gsub!(/cellSize =.*?;/, 'cellSize = 14;') # this is specific for calendar view
  html
end

def merge_into(template_file, files)
  html = File.read(template_file)
  files.each { |file|
    p "Processing #{file}"
    style, script = extract_content_from(file)

    html.insert(html.end_rindex("</style>"), remove_margin_style(style))
    html.insert(html.end_rindex("</script>"), reduce_width(remove_header_span(script)))
  }
  File.open(template_file, "w") { |f| f.write(html) }
end


merge_into('idea.html', [
    '/Users/dima/Documents/idea-upto-21-09-2013/Change size chart.html',
    '/Users/dima/Documents/idea-upto-21-09-2013/Amount of committers.html',
    '/Users/dima/Documents/idea-2012-2013/Files changed in the same commit.html',
    '/Users/dima/Documents/idea-2012-2013/Committers changing same files.html',
    #'/Users/dima/Documents/idea-2012-2013/Amount of commits treemap.html',
    '/Users/dima/Documents/idea-upto-21-09-2013/Commit messages word cloud.html',
    '/Users/dima/Documents/idea-upto-21-09-2013/Commit time punchcard.html',
    '/Users/dima/Documents/idea-upto-21-09-2013/Time between commits histogram.html',
    '/Users/dima/Documents/idea-upto-21-09-2013/Changes calendar view.html',
])