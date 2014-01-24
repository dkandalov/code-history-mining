#!/usr/bin/ruby

class String
  def end_index(s)
    index(s) + s.size
  end
  def end_rindex(s)
    rindex(s) + s.size
  end
end

def extract_content_from(file_name)
  html = File.read(file_name)

  from = html.index('<style')
  to = html.end_index('</style>')
  style = html[from..to]

  from = html.rindex('<script')
  to = html.end_rindex('</script>')
  script = html[from..to-1]

  [style, script]
end

remove_margin_style = Proc.new { |html|
  html.gsub!(/margin:.*?;/, '')
}

remove_header_span = Proc.new { |html|
  html.gsub!(/var header.*?;/m, '')
  html.gsub!(/headerSpan\..*?;/m, '')
  html.gsub!(/[\s\t]header\..*?;/m, '')
}

reduce_width = Proc.new { |html|
  html.gsub!(/var width =.*?;/, 'var width = 800;')
}

change_size_chart_fixes = Proc.new { |html|
  html.gsub!(/width =.*?,/, 'width = 740,')
  html.gsub!('var defaultTimeInterval = "monday";', 'var defaultTimeInterval = "month";')
  html.gsub!('dropDown.append("option").attr("value", "1").html("lines");', '')
  html.gsub!('dropDown.append("option").attr("value", "2").html("characters");', '')
  html.gsub!('return svgPos.left + margin.left', 'return margin.left')
}

amount_of_committers_fixes = Proc.new { |html|
  html.gsub!(/width =.*?,/, 'width = 740,')
  html.gsub!(/var rawDataIndex = 0;/, 'var rawDataIndex = 2;')
}

avg_amount_of_files_fixes = Proc.new { |html|
  html.gsub!(/width =.*?,/, 'width = 740,')
  html.gsub!('var defaultPercentile = 1.0;', 'var defaultPercentile = 0.975;')
  html.gsub!(/var rawDataIndex = 0;/, 'var rawDataIndex = 2;')
}

file_in_same_commit_fixes = Proc.new{ |html|
  html.gsub!(/width =.*?,/, 'width = 800,')
  html.gsub!(/var gravity = gravityValues.*?;/, 'var gravity = gravityValues.Low;')
}

committer_and_files_fixes = Proc.new{ |html|
  html.gsub!(/width =.*?,/, 'width = 800,')
  html.gsub!(/var gravity = gravityValues.*?;/, 'var gravity = gravityValues.Low;')
}

treemap_fixes = Proc.new{ |html|
  html.gsub!(/var w =.*?,/, 'var w = 800,')
  html.gsub!(/font:.*?;/, '')
}

punchcard_fixes = Proc.new{ |html|
  html.gsub!(/width =.*?,/, 'width = 740,')
  html.gsub!('var defaultCommitSizeMultiplier = 1;', 'var defaultCommitSizeMultiplier = 2;')
}

histogram_fixes = Proc.new{ |html|
  html.gsub!(/width =.*?,/, 'width = 740,')
  html.gsub!(/var defaultPercentile =.*?;/, 'var defaultPercentile = 0.8;')
}

word_cloud_fixes = Proc.new{ |html|
  html.gsub!('normalizeWordSize(data.words);', 'excludeWords(["com", "stu", "signed", "stuart", "thinkrelevance", "halloway"]); normalizeWordSize(data.words);')
}


def merge_into(template_file, target_file, files_with_fixes)
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
  File.open(target_file, "w") { |f| f.write(html) }
end


common_fixes = [remove_margin_style, remove_header_span, reduce_width]
base_path = '/Users/dima/Google Drive/visualisations/clojure-code-history/'
merge_into('../clojure-template.html', '../clojure.html', {
    base_path + '/Change size chart.html' => common_fixes + [change_size_chart_fixes],
    base_path + '/Amount of committers.html' => common_fixes + [amount_of_committers_fixes],
    base_path + '/Average amount of files in commit.html' => common_fixes + [avg_amount_of_files_fixes],
    base_path + '/Files changed in the same commit.html' => common_fixes + [file_in_same_commit_fixes],
    base_path + '/Committers changing same files.html' => common_fixes + [committer_and_files_fixes],
    base_path + '/Amount of commits treemap.html' => common_fixes + [treemap_fixes],
    base_path + '/Commit time punchcard.html' => common_fixes + [punchcard_fixes],
    base_path + '/Time between commits histogram.html' => common_fixes + [histogram_fixes],
    base_path + '/Commit messages word cloud.html' => common_fixes + [word_cloud_fixes],
})