#!/usr/bin/ruby
require './merge.rb'
extend VisualizationsConfig

class Template < CodeHistoryTemplate
  def project_name
    'Scala'
  end

  def full_project_name
    'Scala programming language'
  end

  def url_to_project_page
    'https://github.com/scala/scala‎'
  end

  def code_history_dates
    ' from 03/01/2005 to 19/12/2013'
  end

  def google_drive_url
    'https://drive.google.com/#folders/0B5PfR1lF8o5SNWpwUDZJbERoMEk'
  end
end

src_path = '/Users/dima/Google Drive/visualisations/scala-code-history/'
merge_visualizations(src_path, Template, 'scala', {
    'Change size chart.html' => [with_change_size_chart(grouped_by = 'month', moving_average = true)],
    'Amount of committers.html' => [with_amount_of_committers(grouped_by = 'month')],
    'Average amount of files in commit.html' => [with_avg_amount_of_files(percentile = '0.975', grouped_by = 'month')],
    'Files changed in the same commit.html' => [with_files_graph(gravity = 'High', min_link = 17)],
    'Committers changing same files.html' => [with_committers_and_files_graph(gravity = 'Low', min_cluster = '4', min_link = '15')],
    'Amount of commits treemap.html' => [with_treemap],
    'Commit time punchcard.html' => [with_punchcard(multiplier = '2')],
    'Time between commits histogram.html' => [with_histogram(percentile = '0.8')],
    'Commit messages word cloud.html' => [with_wordcloud(exclusions  ='"si"')],
})