#!/usr/bin/ruby
require './merge.rb'
extend VisualizationsConfig

class Template < CodeHistoryTemplate
  def project_name
    'GHC'
  end

  def full_project_name
    'Glasgow Haskell Compiler'
  end

  def url_to_project_page
    'https://github.com/ghc/ghc'
  end

  def code_history_dates
    ' from January 2006 to January 2014'
  end

  def google_drive_url
    'https://drive.google.com/#folders/0B5PfR1lF8o5SaC1ncG84V29wQTQ'
  end
end

src_path = '/Users/dima/Google Drive/visualisations/ghc/'
merge_visualizations(src_path, Template, 'ghc', {
    'Change size chart.html' => [with_change_size_chart(grouped_by = 'month', moving_average = false)],
    'Amount of committers.html' => [with_amount_of_committers(grouped_by = 'month')],
    'Average amount of files in commit.html' => [with_avg_amount_of_files(percentile = '0.95', grouped_by = 'week')],
    'Files changed in the same commit.html' => [with_files_graph(gravity = 'Medium', min_link = '10')],
    'Committers changing same files.html' => [with_committers_and_files_graph(gravity = 'Low', min_cluster = '2', min_link = '10')],
    'Amount of commits treemap.html' => [with_treemap],
    'Commit time punchcard.html' => [with_punchcard(multiplier = '2')],
    'Time between commits histogram.html' => [with_histogram(percentile = '0.6')],
    'Commit messages word cloud.html' => [with_wordcloud(exclusions = '')],
})