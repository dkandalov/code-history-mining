#!/usr/bin/ruby
require './merge.rb'

change_size_chart_fixes = Proc.new { |html|
  html.gsub!(/width =.*?,/, 'width = 740,')
  html.gsub!('dropDown.append("option").attr("value", "1").html("lines");', '')
  html.gsub!('dropDown.append("option").attr("value", "2").html("characters");', '')
  html.gsub!('return svgPos.left + margin.left', 'return margin.left')
  html.gsub!('var showOneMonthMean = false;', 'var showOneMonthMean = true;')
}

amount_of_committers_fixes = Proc.new { |html|
  html.gsub!(/width =.*?,/, 'width = 740,')
  html.gsub!(/var rawDataIndex =.*?;/, 'var rawDataIndex = 2;')
  html.gsub!(/var timeInterval =.*?;/, 'var timeInterval = d3.time.month;')
}

file_in_same_commit_fixes = Proc.new{ |html|
  html.gsub!(/width =.*?,/, 'width = 800,')
  html.gsub!(/var gravity = gravityValues.*?;/, 'var gravity = gravityValues.High;')
}

committer_and_files_fixes = Proc.new{ |html|
  html.gsub!(/width =.*?,/, 'width = 800,')
  html.gsub!('var minLinkStrength = linkValuesExtent[0];', 'var minLinkStrength = 7;')
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

calendar_fixes = Proc.new{ |html|
  html.gsub!(/cellSize =.*?;/, 'cellSize = 14;')
  html.gsub!(/width =.*?,/, 'width = 800,')
  html.gsub!('#body {', '#calendar-view {')

  html.gsub!('dropDown.append("option").attr("value", "1").html("lines");', '')
  html.gsub!('dropDown.append("option").attr("value", "2").html("characters");', '')

  html.gsub!(/.+2004,.+\\n\\\n/, '') # exclude 2004 just because it looks white for IntelliJ compared to other years
}

word_cloud_fixes = Proc.new{ |html|
  html.gsub!('normalizeWordSize(data.words);', 'excludeWords(["idea", "ideadev"]); normalizeWordSize(data.words);')
}

src_path = '/Users/dima/Google Drive/visualisations/'
merge_visualizations(src_path, 'idea', {
    'idea-upto-21-09-2013/Change size chart.html' => [change_size_chart_fixes],
    'idea-upto-21-09-2013/Amount of committers.html' => [amount_of_committers_fixes],
    'idea-2012-2013/Files changed in the same commit.html' => [file_in_same_commit_fixes],
    'idea-2012-2013/Committers changing same files.html' => [committer_and_files_fixes],
    'idea-2012-2013/Amount of commits treemap.html' => [treemap_fixes],
    'idea-upto-21-09-2013/Commit time punchcard.html' => [punchcard_fixes],
    'idea-upto-21-09-2013/Time between commits histogram.html' => [histogram_fixes],
    'idea-upto-21-09-2013/Commit messages word cloud.html' => [word_cloud_fixes],
    'idea-upto-21-09-2013/Changes calendar view.html' => [calendar_fixes],
})