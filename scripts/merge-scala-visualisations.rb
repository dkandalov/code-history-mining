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
  html.gsub!(/var rawDataIndex = 0;/, 'var rawDataIndex = 2;')
}

file_in_same_commit_fixes = Proc.new{ |html|
  html.gsub!(/width =.*?,/, 'width = 800,')
  html.gsub!(/var gravity = gravityValues.*?;/, 'var gravity = gravityValues.High;')
  html.gsub!('var minLinkStrength = linkValuesExtent[0];', 'var minLinkStrength = 17;')
}

committer_and_files_fixes = Proc.new{ |html|
  html.gsub!(/width =.*?,/, 'width = 800,')
  html.gsub!('var minClusterSize = 2;', 'var minClusterSize = 4;')
  html.gsub!('var minLinkStrength = linkValuesExtent[0];', 'var minLinkStrength = 15;')
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
  html.gsub!('normalizeWordSize(data.words);', 'excludeWords(["si"]); normalizeWordSize(data.words);')
}

src_path = '/Users/dima/Google Drive/visualisations/scala-code-history/'
merge_into(src_path, '../scala-template.html', '../scala.html', {
    '/Change size chart.html' => [change_size_chart_fixes],
    '/Amount of committers.html' => [amount_of_committers_fixes],
    '/Files changed in the same commit.html' => [file_in_same_commit_fixes],
    '/Committers changing same files.html' => [committer_and_files_fixes],
    '/Amount of commits treemap.html' => [treemap_fixes],
    '/Commit time punchcard.html' => [punchcard_fixes],
    '/Time between commits histogram.html' => [histogram_fixes],
    '/Commit messages word cloud.html' => [word_cloud_fixes],
})