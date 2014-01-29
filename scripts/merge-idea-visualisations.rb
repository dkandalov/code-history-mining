#!/usr/bin/ruby
require './merge.rb'
extend VisualizationsConfig

class Template < CodeHistoryTemplate
  def project_name
    'IntelliJ'
  end

  def full_project_name
    'IntelliJ community edition'
  end

  def url_to_project_page
    'https://github.com/JetBrains/intellij-community'
  end

  def google_drive_url
    'https://drive.google.com/#folders/0B5PfR1lF8o5SRUlqRkt5Ylptelk'
  end

  def change_size_chart_comment
    '<br/>Note downward trends around New Year and spikes in November/December which somewhat correlate with major releases.'
  end
end


src_path = '/Users/dima/Google Drive/visualisations/'
merge_visualizations(src_path, Template, 'idea', {
    'idea-upto-21-09-2013/Change size chart.html' => [with_change_size_chart(grouped_by = 'month', moving_average = true)],
    'idea-upto-21-09-2013/Amount of committers.html' => [with_amount_of_committers(grouped_by = 'month')],
    'idea-upto-21-09-2013/Average amount of files in commit.html' => [with_avg_amount_of_files(percentile = '0.95', grouped_by = 'day')],
    'idea-2012-2013/Files changed in the same commit.html' => [with_files_graph(gravity = 'High', min_link = 8)],
    'idea-2012-2013/Committers changing same files.html' => [with_committers_and_files_graph(gravity = 'Medium', min_cluster = '2', min_link = '7')],
    'idea-2012-2013/Amount of commits treemap.html' => [with_treemap],
    'idea-upto-21-09-2013/Commit time punchcard.html' => [with_punchcard(multiplier = '2')],
    'idea-upto-21-09-2013/Time between commits histogram.html' => [with_histogram(percentile = '0.8')],
    'idea-upto-21-09-2013/Commit messages word cloud.html' => [with_wordcloud(exclusions ='"idea", "ideadev"')]
})