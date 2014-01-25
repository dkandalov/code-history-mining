#!/usr/bin/ruby

class CodeHistoryTemplate < Mustache
  def project_name
    'TODO'
  end

  def full_project_name
    project_name
  end

  def url_to_project_page
    'TODO'
  end

  def code_history_dates
    ''
  end

  def google_drive_url
    'TODO'
  end

  def committers_files_graph_comment
    ''
  end

  def wordcloud_comment
    ''
  end
end

module VisualizationsConfig
  def with_change_size_chart(grouped_by = nil, moving_average = false)
    Proc.new { |html|
      html.gsub!(/width =.*?,/, 'width = 740,')
      html.gsub!('var defaultTimeInterval = "monday";', "var defaultTimeInterval = \"#{grouped_by}\";") unless grouped_by.nil?
      html.gsub!('var showMovingAverage = false;', "var showMovingAverage = #{moving_average};")
      html.gsub!('dropDown.append("option").attr("value", "1").html("lines");', '')
      html.gsub!('dropDown.append("option").attr("value", "2").html("characters");', '')
      html.gsub!('return svgPos.left + margin.left', 'return margin.left')
    }
  end

  def with_amount_of_committers(grouped_by = nil)
    Proc.new { |html|
      html.gsub!(/width =.*?,/, 'width = 740,')
      unless grouped_by.nil?
        i = ['day', 'week', 'month'].index(grouped_by)
        html.gsub!(/var rawDataIndex = 0;/, "var rawDataIndex = #{i};")
      end
    }
  end

  def with_avg_amount_of_files(percentile = nil, grouped_by = nil, moving_average = false)
    Proc.new { |html|
      html.gsub!(/width =.*?,/, 'width = 740,')
      html.gsub!('var showMovingAverage = false;', "var showMovingAverage = #{moving_average};")
      html.gsub!('var defaultPercentile = 1.0;', "var defaultPercentile = #{percentile};")
      unless grouped_by.nil?
        i = ['day', 'week', 'month'].index(grouped_by)
        html.gsub!(/var rawDataIndex = 0;/, "var rawDataIndex = #{i};")
      end
    }
  end

  def with_files_graph(gravity = nil, min_link = nil)
    Proc.new{ |html|
      html.gsub!(/width =.*?,/, 'width = 800,')
      html.gsub!(/var gravity = gravityValues.*?;/, "var gravity = gravityValues.#{gravity};") unless gravity.nil?
      html.gsub!('var minLinkStrength = linkValuesExtent[0];', "var minLinkStrength = #{min_link};") unless min_link.nil?
    }
  end

  def with_committers_and_files_graph(gravity = nil, min_cluster = nil, min_link = nil)
    Proc.new{ |html|
      html.gsub!(/width =.*?,/, 'width = 800,')
      html.gsub!(/var gravity = gravityValues.*?;/, "var gravity = gravityValues.#{gravity};") unless gravity.nil?
      html.gsub!('var minClusterSize = 2;', "var minClusterSize = #{min_cluster};") unless min_cluster.nil?
      html.gsub!('var minLinkStrength = linkValuesExtent[0];', "var minLinkStrength = #{min_link};") unless min_link.nil?
    }
  end

  def with_treemap
    Proc.new{ |html|
      html.gsub!(/var w =.*?,/, 'var w = 800,')
      html.gsub!(/font:.*?;/, '')
    }
  end

  def with_punchcard(multiplier = nil)
    Proc.new{ |html|
      html.gsub!(/width =.*?,/, 'width = 740,')
      html.gsub!('var defaultCommitSizeMultiplier = 1;', 'var defaultCommitSizeMultiplier = 2;') unless multiplier.nil?
    }
  end

  def with_histogram(percentile = nil)
    Proc.new{ |html|
      html.gsub!(/width =.*?,/, 'width = 740,')
      html.gsub!(/var defaultPercentile =.*?;/, "var defaultPercentile = #{percentile};") unless percentile.nil?
    }
  end

  def with_wordcloud(exclusions = nil)
    Proc.new{ |html|
      html.gsub!('normalizeWordSize(data.words);', "excludeWords([#{exclusions}]); normalizeWordSize(data.words);") unless exclusions.nil?
    }
  end
end


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


def merge_visualizations(src_path, project_name, fixes_by_filename)
  do_merge(src_path, "../#{project_name}-template.html", "../#{project_name}.html", fixes_by_filename)
end


def do_merge(src_path, template_file, target_file, fixes_by_filename)
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
  common_fixes = [remove_margin_style, remove_header_span, reduce_width]


  html = File.read(template_file)
  fixes_by_filename.each { |filename, fixes|
    p "Processing #{filename}"
    style, script = extract_content_from(src_path + '/' + filename)

    (fixes + common_fixes).each { |fix|
      fix.call(style)
      fix.call(script)
    }

    html.insert(html.end_rindex("</style>"), style)
    html.insert(html.end_rindex("</script>"), script)
  }
  File.open(target_file, "w") { |f| f.write(html) }
end
