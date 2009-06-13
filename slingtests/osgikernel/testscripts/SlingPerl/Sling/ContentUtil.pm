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
#}}}

#{{{sub add_setup

=pod

=head2 add_setup

Returns a textual representation of the request needed to add content to the
system.

=cut

sub add_setup {
    my ( $baseURL, $remoteDest, $properties ) = @_;
    die "No base URL provided!" unless defined $baseURL;
    die "No position or ID to perform action for specified!" unless defined $remoteDest;
    my $property_post_vars;
    foreach my $property ( @{ $properties } ) {
        $property =~ /^([^=]*)=(.*)/;
	if ( defined $1 && defined $2 ) {
            $property_post_vars .= "'$1','$2',";
	}
    }
    $property_post_vars =~ s/,$//;
    my $postVariables = "\$postVariables = [$property_post_vars]";
    return "post $baseURL/$remoteDest $postVariables";
}
#}}}

#{{{sub add_eval

=pod

=head2 add_eval

Check result of adding content.

=cut

sub add_eval {
    my ( $res ) = @_;
    return ( $$res->code =~ /^20(0|1)$/ );
}
#}}}

#{{{sub delete_setup

=pod

=head2 delete_setup

Returns a textual representation of the request needed to delete content from
the system.

=cut

sub delete_setup {
    my ( $baseURL, $remoteDest ) = @_;
    die "No base url defined!" unless defined $baseURL;
    die "No content destination to delete defined!" unless defined $remoteDest;
    my $postVariables = "\$postVariables = [':operation','delete']";
    return "post $baseURL/$remoteDest $postVariables";
}
#}}}

#{{{sub delete_eval

=pod

=head2 delete_eval

Inspects the result returned from issuing the request generated in delete_setup
returning true if the result indicates the content was deleted successfully,
else false.

=cut

sub delete_eval {
    my ( $res ) = @_;
    return ( $$res->code =~ /^200$/ );
}
#}}}

#{{{sub exists_setup

=pod

=head2 exists_setup

Returns a textual representation of the request needed to test whether content
exists in the system.

=cut

sub exists_setup {
    my ( $baseURL, $remoteDest ) = @_;
    die "No base url defined!" unless defined $baseURL;
    die "No position or ID to perform exists for specified!" unless defined $remoteDest;
    return "get $baseURL/$remoteDest.json";
}
#}}}

#{{{sub exists_eval

=pod

=head2 exists_eval

Inspects the result returned from issuing the request generated in exists_setup
returning true if the result indicates the content does exist in the system,
else false.

=cut

sub exists_eval {
    my ( $res ) = @_;
    return ( $$res->code =~ /^200$/ );
}
#}}}

#{{{sub upload_file_setup

=pod

=head2 upload_file_setup

Returns a textual representation of the request needed to upload a file to the system.

=cut

sub upload_file_setup {
    my ( $baseURL, $localPath, $remoteDest, $filename ) = @_;
    die "No base URL provided to upload against!" unless defined $baseURL;
    die "No local file to upload defined!" unless defined $localPath;
    die "No remote path to upload to defined for file $localPath!" unless defined $remoteDest;
    $filename = "./*" unless ( $filename !~ /^$/ );
    my $postVariables = "\$postVariables = []";
    return "fileupload $baseURL/$remoteDest $filename $localPath $postVariables";
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
