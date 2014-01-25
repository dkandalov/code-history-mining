#!/usr/bin/ruby
require './merge.rb'
extend VisualizationsConfig

# caledar was removed from plugin itself, left it here for fun
calendar_fixes = Proc.new{ |html|
  html.gsub!(/cellSize =.*?;/, 'cellSize = 14;')
  html.gsub!(/width =.*?,/, 'width = 800,')
  html.gsub!('#body {', '#calendar-view {')

  html.gsub!('dropDown.append("option").attr("value", "1").html("lines");', '')
  html.gsub!('dropDown.append("option").attr("value", "2").html("characters");', '')

  html.gsub!(/.+2004,.+\\n\\\n/, '') # exclude 2004 just because it looks white for IntelliJ compared to other years
}

src_path = '/Users/dima/Google Drive/visualisations/'
merge_visualizations(src_path, 'idea', {
    'idea-upto-21-09-2013/Change size chart.html' => [with_change_size_chart(grouped_by = 'month', moving_average = true)],
    'idea-upto-21-09-2013/Amount of committers.html' => [with_amount_of_committers(grouped_by = 'month')],
    'idea-upto-21-09-2013/Average amount of files in commit.html' => [with_avg_amount_of_files(percentile = '0.95', grouped_by = 'day')],
    'idea-2012-2013/Files changed in the same commit.html' => [with_files_graph(gravity = 'High', min_link = 8)],
    'idea-2012-2013/Committers changing same files.html' => [with_committers_and_files_graph(gravity = 'Medium', min_cluster = '2', min_link = '7')],
    'idea-2012-2013/Amount of commits treemap.html' => [with_treemap],
    'idea-upto-21-09-2013/Commit time punchcard.html' => [with_punchcard(multiplier = '2')],
    'idea-upto-21-09-2013/Time between commits histogram.html' => [with_histogram(percentile = '0.8')],
    'idea-upto-21-09-2013/Commit messages word cloud.html' => [with_wordcloud(exclusions ='"idea", "ideadev"')],
    'idea-upto-21-09-2013/Changes calendar view.html' => [calendar_fixes],
})