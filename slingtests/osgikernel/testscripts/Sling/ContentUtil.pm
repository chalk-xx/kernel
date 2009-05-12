#!/usr/bin/perl

package Sling::ContentUtil;

=head1 NAME

ContentUtil - Utility library returning strings representing Rest queries that
perform content operations in the system.

=head1 ABSTRACT

ContentUtil perl library essentially provides the request strings needed to
interact with content functionality exposed over the system rest interfaces.

Each interaction has a setup and eval method. setup provides the request,
whilst eval interprets the response to give further information about the
result of performing the request.

=cut

#{{{imports
use strict;
use lib qw ( .. );
use Sling::Util;
#}}}

#{{{sub upload_file_setup

=pod

=head2 upload_file_setup

Returns a textual representation of the request needed to upload a file to the system.

=cut

sub upload_file_setup {
    my ( $baseURL, $localPath, $remotePath, $filename ) = @_;
    die "No base URL provided to upload against!" unless defined $baseURL;
    die "No local file to upload defined!" unless defined $localPath;
    die "No remote path to upload to defined for file $localPath!" unless defined $remotePath;
    $filename = "./*" unless ( $filename !~ /^$/ );
    my $postVariables = "\$postVariables = []";
    return "fileupload $baseURL/$remotePath?sling:authRequestLogin=1 $filename $localPath $postVariables";
}
#}}}

#{{{sub upload_file_eval

=pod

=head2 upload_file_eval

Check result of system upload_file.

=cut

sub upload_file_eval {
    my ( $res ) = @_;
    return ( $$res->code =~ /^20(0|1)$/ );
}
#}}}

1;
