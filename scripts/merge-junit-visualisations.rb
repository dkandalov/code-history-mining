#!/usr/bin/ruby
require './merge.rb'
extend VisualizationsConfig

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