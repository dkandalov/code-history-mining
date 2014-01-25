#!/usr/bin/ruby
require 'mustache'
require './merge.rb'
extend VisualizationsConfig

class Template < CodeHistoryTemplate
  self.template_file = '../template.html'

  def project_name
    'node.js'
  end

  def url_to_project_page
    'https://github.com/joyent/node'
  end

  def code_history_dates
    ' from February 2009 to January 2014'
  end

  def google_drive_url
    'https://drive.google.com/#folders/0B5PfR1lF8o5SS01PdWtPUk5tQ1E'
  end
end
File.open("../nodejs-template.html", "w"){ |f| f.write(Template.render) }

src_path = '/Users/dima/Google Drive/visualisations/nodejs/'
merge_visualizations(src_path, 'nodejs', {
    'Change size chart.html' => [with_change_size_chart(grouped_by = 'week', moving_average = false)],
    'Amount of committers.html' => [with_amount_of_committers(grouped_by = 'week')],
    'Average amount of files in commit.html' => [with_avg_amount_of_files(percentile = '0.98', grouped_by = 'week')],
    'Files changed in the same commit.html' => [with_files_graph(gravity = 'High', min_link = '30')],
    'Committers changing same files.html' => [with_committers_and_files_graph(gravity = 'Low', min_cluster = '2', min_link = '10')],
    'Amount of commits treemap.html' => [with_treemap],
    'Commit time punchcard.html' => [with_punchcard(multiplier = '2')],
    'Time between commits histogram.html' => [with_histogram(percentile = '0.6')],
    'Commit messages word cloud.html' => [with_wordcloud(exclusions = '')],
})