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

remove_margin_style = Proc.new { |html|
  html.gsub!(/margin:.*?;/, '')
}

remove_header_span = Proc.new { |html|
  html.gsub!(/var header.*?;/m, '')
  html.gsub!(/headerSpan\..*?;/m, '')
  html.gsub!(/header\..*?;/m, '')
}

reduce_width = Proc.new { |html|
  html.gsub!(/var width =.*?;/, 'var width = 800;')
}

change_size_chart_fixes = Proc.new { |html|
  html.gsub!(/width =.*?,/, 'width = 740,')
}

amount_of_committers_fixes = Proc.new { |html|
  html.gsub!(/width =.*?,/, 'width = 740,')
}

file_in_same_commit_fixes = Proc.new{ |html|
  html.gsub!(/width =.*?,/, 'width = 740,')
}

committer_and_files_fixes = Proc.new{ |html|
  html.gsub!(/width =.*?,/, 'width = 800,')
}

punchcard_fixes = Proc.new{ |html|
  html.gsub!(/width =.*?,/, 'width = 800,')
}

histogram_fixes = Proc.new{ |html|
  html.gsub!(/width =.*?,/, 'width = 740,')
}

calendar_fixes = Proc.new{ |html|
  html.gsub!(/cellSize =.*?;/, 'cellSize = 14;') # this is specific for calendar view
  html.gsub!(/width =.*?,/, 'width = 800,')
}

def merge_into(template_file, files_with_fixes)
  html = File.read(template_file)
  files_with_fixes.each { |file, fixes|
    p "Processing #{file}"
    style, script = extract_content_from(file)

    fixes.each { |fix|
      fix.call(style)
      fix.call(script)
    }

    html.insert(html.end_rindex("</style>"), style)
    html.insert(html.end_rindex("</script>"), script)
  }
  File.open(template_file, "w") { |f| f.write(html) }
end


common = [remove_margin_style, remove_header_span, reduce_width]
merge_into('idea.html', {
    #'/Users/dima/Documents/idea-upto-21-09-2013/Change size chart.html' => common + [change_size_chart_fixes],
    #'/Users/dima/Documents/idea-upto-21-09-2013/Amount of committers.html' => common + [amount_of_committers_fixes],
    #'/Users/dima/Documents/idea-2012-2013/Files changed in the same commit.html' => common + [file_in_same_commit_fixes],
    #'/Users/dima/Documents/idea-2012-2013/Committers changing same files.html' => common + [committer_and_files_fixes],
    ##'/Users/dima/Documents/idea-2012-2013/Amount of commits treemap.html',
    #'/Users/dima/Documents/idea-upto-21-09-2013/Commit time punchcard.html' => common + [punchcard_fixes],
    #'/Users/dima/Documents/idea-upto-21-09-2013/Time between commits histogram.html' => common + [histogram_fixes],
    #'/Users/dima/Documents/idea-upto-21-09-2013/Commit messages word cloud.html' => common,
    #'/Users/dima/Documents/idea-upto-21-09-2013/Changes calendar view.html' => common + [calendar_fixes],
})