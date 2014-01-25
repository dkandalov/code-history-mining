#!/usr/bin/ruby
require 'mustache'
require './merge.rb'
extend VisualizationsConfig

class Template < CodeHistoryTemplate
  self.template_file = '../template.html'

  def project_name
    'Ruby'
  end

  def full_project_name
    'Ruby programming language'
  end

  def url_to_project_page
    'https://github.com/ghc/ghc'
  end

  def code_history_dates
    ' from January 2006 to January 2014'
  end

  def google_drive_url
    'https://github.com/ruby/ruby'
  end

  def files_graph_comment
    '<br/>(Because of many changes to ChangeLog file it was excluded from this graph.)'
  end

  def committers_files_graph_comment
    '<br/>(Because of many changes to ChangeLog file it was excluded from this graph.)'
  end

end
File.open("../ruby-template.html", "w"){ |f| f.write(Template.render) }

src_path = '/Users/dima/Google Drive/visualisations/ruby/'
merge_visualizations(src_path, 'ruby', {
    'Change size chart.html' => [with_change_size_chart(grouped_by = 'month', moving_average = false)],
    'Amount of committers.html' => [with_amount_of_committers(grouped_by = 'month')],
    'Average amount of files in commit.html' => [with_avg_amount_of_files(percentile = '0.98', grouped_by = 'month')],
    'Files changed in the same commit.html' => [with_files_graph(gravity = 'Medium', min_link = '19')],
    'Committers changing same files.html' => [with_committers_and_files_graph(gravity = 'Low', min_cluster = '2', min_link = '20')],
    'Amount of commits treemap.html' => [with_treemap],
    'Commit time punchcard.html' => [with_punchcard(multiplier = '2')],
    'Time between commits histogram.html' => [with_histogram(percentile = '0.6')],
    'Commit messages word cloud.html' => [with_wordcloud(exclusions = '"svn", "ruby", "org", "trunk", "git", "dd", "ci", "fe", "id", "rb", "ssh", "lang"')],
})