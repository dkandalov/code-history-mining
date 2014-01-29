#!/usr/bin/ruby
require './merge.rb'
extend VisualizationsConfig

class Template < CodeHistoryTemplate
  def project_name
    'Ruby on Rails'
  end

  def url_to_project_page
    'https://github.com/rails/rails‎'
  end

  def code_history_dates
    ' from November 2004 to January 2014'
  end

  def google_drive_url
    'https://drive.google.com/#folders/0B5PfR1lF8o5SWGRYYmlISmZtUU0'
  end
end

src_path = '/Users/dima/Google Drive/visualisations/rails/'
merge_visualizations(src_path, Template, 'rails', {
    'Change size chart.html' => [with_change_size_chart(grouped_by = 'month')],
    'Amount of committers.html' => [with_amount_of_committers(grouped_by = 'month')],
    'Average amount of files in commit.html' => [with_avg_amount_of_files(percentile = '0.975', grouped_by = 'month')],
    'Files changed in the same commit.html' => [with_files_graph(gravity = 'High', min_link = 20)],
    'Committers changing same files.html' => [with_committers_and_files_graph(gravity = 'Low', min_cluster = '4', min_link = '23')],
    'Amount of commits treemap.html' => [with_treemap],
    'Commit time punchcard.html' => [with_punchcard(multiplier = '2')],
    'Time between commits histogram.html' => [with_histogram(percentile = '0.6')],
    'Commit messages word cloud.html' => [with_wordcloud(exclusions ='"svn", "http", "org", "commit", "rails", "trunk", "rubyonrails", "git", "ee", "ecf", "de", "fe", "id", "com"')],
})