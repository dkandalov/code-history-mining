#!/usr/bin/ruby
require 'mustache'
require './merge.rb'
extend VisualizationsConfig

class Template < Mustache
  def project_name
    'JUnit'
  end

  def url_to_project_page
    'https://github.com/junit-team/junit'
  end

  def code_history_dates
    ' from January 2001 to September 2013'
  end

  def google_drive_url
    'https://drive.google.com/#folders/0B5PfR1lF8o5SbUZzV1RYTC1GcDQ'
  end

  def committers_files_graph_comment
    '<br/>This particular graph is not very accurate because of different
     VCS user names for the same person (e.g. "dsaff" and "David Saff").'
  end

  def wordcloud_comment
    '<br/>
        This particular cloud might not be very representative because of commit messages
        with meta-information (that\'s why cloud has "threeriversinstitute" in it).
        You can alt-click on words to exclude them.'
  end
end
Template.template_file = '../template.html'
File.open("../junit-template.html", "w"){ |f| f.write(Template.render) }

src_path = '/Users/dima/Google Drive/visualisations/junit/'
merge_visualizations(src_path, 'junit', {
    'Change size chart.html' => [with_change_size_chart(grouped_by = 'month', moving_average = false)],
    'Amount of committers.html' => [with_amount_of_committers(grouped_by = 'month')],
    'Average amount of files in commit.html' => [with_avg_amount_of_files(percentile = '0.95', grouped_by = 'month')],
    'Files changed in the same commit.html' => [with_files_graph(gravity = 'Medium', min_link = '8')],
    'Committers changing same files.html' => [with_committers_and_files_graph(gravity = 'Low', min_cluster = '2', min_link = '1')],
    'Amount of commits treemap.html' => [with_treemap],
    'Commit time punchcard.html' => [with_punchcard(multiplier = '2')],
    'Time between commits histogram.html' => [with_histogram(percentile = '0.6')],
    'Commit messages word cloud.html' => [with_wordcloud(exclusions = '"svn", "http", "org", "commit", "rails", "trunk", "rubyonrails", "git", "ee", "ecf", "de", "fe", "id", "com"')],
})