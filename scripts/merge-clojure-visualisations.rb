#!/usr/bin/ruby
require './merge.rb'
extend VisualizationsConfig

class Template < CodeHistoryTemplate
  def project_name
    'Clojure'
  end

  def full_project_name
    'Clojure programming language'
  end

  def url_to_project_page
    'https://github.com/clojure/clojure'
  end

  def google_drive_url
    'https://drive.google.com/#folders/0B5PfR1lF8o5SZ1RNUjloYldWeFE'
  end

  def code_history_dates
    ' from 24/03/2006 to 23/11/2013'
  end
end

src_path = '/Users/dima/Google Drive/visualisations/clojure-code-history/'
merge_visualizations(src_path, Template, 'clojure', {
    'Change size chart.html' => [with_change_size_chart(grouped_by = 'month')],
    'Amount of committers.html' => [with_amount_of_committers(grouped_by = 'month')],
    'Average amount of files in commit.html' => [with_avg_amount_of_files(percentile = '0.975', grouped_by = 'month')],
    'Files changed in the same commit.html' => [with_files_graph(gravity = 'Low')],
    'Committers changing same files.html' => [with_committers_and_files_graph(gravity = 'Low')],
    'Amount of commits treemap.html' => [with_treemap],
    'Commit time punchcard.html' => [with_punchcard(multiplier = '2')],
    'Time between commits histogram.html' => [with_histogram(percentile = '0.8')],
    'Commit messages word cloud.html' => [with_wordcloud(exclusions  ='"com", "stu", "signed", "stuart", "thinkrelevance", "halloway"')],
})