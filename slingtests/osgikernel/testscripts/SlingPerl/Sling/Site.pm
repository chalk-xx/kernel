#!/usr/bin/perl

package Sling::Site;

=head1 NAME

Site - site related functionality for Sakai implemented over rest
APIs.

=head1 ABSTRACT

Perl library providing a layer of abstraction to the REST site methods

=cut

#{{{imports
use strict;
use lib qw ( .. );
use Text::CSV;
use Sling::SiteUtil;
use Sling::ContentUtil;
use Sling::Print;
use Sling::Request;
#}}}

#{{{sub new

=pod

=head2 new

Create, set up, and return a Site object.

=cut

sub new {
    my ( $class, $url, $lwpUserAgent, $verbose ) = @_;
    die "url not defined!" unless defined $url;
    die "no lwp user agent provided!" unless defined $lwpUserAgent;
    my $response;
    my $site = { BaseURL => "$url",
                 LWP => $lwpUserAgent,
		 Message => "",
		 Owners => "",
		 Response => \$response,
		 Verbose => $verbose};
    bless( $site, $class );
    return $site;
}
#}}}

#{{{sub set_results
sub set_results {
    my ( $site, $message, $response ) = @_;
    $site->{ 'Message' } = $message;
    $site->{ 'Response' } = $response;
    return 1;
}
#}}}

#{{{sub update
sub update {
    my ( $site, $id, $template, $joinable, $other_properties, $log ) = @_;
    my @properties = ("sling:resourceType=sakai/site");
    if ( defined $template ) {
        push ( @properties, "sakai:site-template=$template" );
    }
    if ( defined $joinable ) {
        push ( @properties, "sakai:joinable=$joinable" );
    }
    foreach my $property ( @{ $other_properties } ) {
        push ( @properties, "$property" );
    }
    my $res = Sling::Request::request( \$site,
        Sling::ContentUtil::add_setup( $site->{ 'BaseURL' }, $id, \@properties ) );
    my $success = Sling::ContentUtil::add_eval( $res );
    my $message = ( $success ? "Action successful" : "Action not successful" );
    $message .= " for site \"$id\".";
    $site->set_results( "$message", $res );
    Sling::Print::print_file_lock( $message, $log ) if ( defined $log );
    return $success;
}
#}}}

#{{{sub update_from_file
sub update_from_file {
    my ( $site, $file, $forkId, $numberForks, $log ) = @_;
    my $csv = Text::CSV->new();
    my $count = 0;
    my $numberColumns = 0;
    my @column_headings;
    open ( FILE, $file );
    while ( <FILE> ) {
        if ( $count++ == 0 ) {
	    # Parse file column headings first to determine field names:
	    if ( $csv->parse( $_ ) ) {
	        @column_headings = $csv->fields();
		# First field must be site:
		if ( $column_headings[0] !~ /^[Ss][Ii][Tt][Ee]$/ ) {
		    die "First CSV column must be the site ID, column heading must be \"site\". Found: \"" . $column_headings[0] . "\".\n";
		}
		$numberColumns = @column_headings;
	    }
	    else {
	        die "CSV broken, failed to parse line: " . $csv->error_input;
	    }
	}
        elsif ( $forkId == ( $count++ % $numberForks ) ) {
	    my @properties;
	    if ( $csv->parse( $_ ) ) {
	        my @columns = $csv->fields();
		my $columns_size = @columns;
		# Check row has same number of columns as there were column headings:
		if ( $columns_size != $numberColumns ) {
		    die "Found \"$columns_size\" columns. There should have been \"$numberColumns\".\nRow contents was: $_";
		}
		my $id = $columns[0];
		for ( my $i = 1; $i < $numberColumns ; $i++ ) {
                    my $value = $column_headings[ $i ] . "=" . $columns[ $i ];
		    push ( @properties, $value );
		}
	        my $template; my $joinable;
                $site->update( $id, $template, $joinable, \@properties, $log );
		Sling::Print::print_lock( $site->{ 'Message' } ) if ( ! defined $log );
	    }
	    else {
	        die "CSV broken, failed to parse line: " . $csv->error_input;
	    }
	}
    }
    close ( FILE ); 
    return 1;
}
#}}}

#{{{sub add_member
sub add_member {
    my ( $site, $id, $member, $role, $log ) = @_;
    my $res = Sling::Request::request( \$site,
        Sling::SiteUtil::add_member_setup( $site->{ 'BaseURL' }, $id, $member, $role ) );
    my $success = Sling::SiteUtil::add_member_eval( \$res );
    my $message = "Site \"$id\", member \"$member\" ";
    $message .= ( $success ? "added!" : "was not added!" );
    $site->set_results( "$message", \$res );
    Sling::Print::print_file_lock( $message, $log ) if ( defined $log );
    return $success;
}
#}}}

#{{{sub delete
sub delete {
    my ( $site, $id, $log ) = @_;
    my $res = Sling::Request::request( \$site,
        Sling::ContentUtil::delete_setup( $site->{ 'BaseURL' }, $id ) );
    my $success = Sling::ContentUtil::delete_eval( $res );
    my $message = "Site \"$id\" ";
    $message .= ( $success ? "deleted!" : "was not deleted!" );
    $site->set_results( "$message", $res );
    Sling::Print::print_file_lock( $message, $log ) if ( defined $log );
    return $success;
}
#}}}

#{{{sub exists
sub exists {
    my ( $site, $id, $log ) = @_;
    my $res = Sling::Request::request( \$site,
        Sling::ContentUtil::exists_setup( $site->{ 'BaseURL' }, $id ) );
    my $exists = Sling::ContentUtil::exists_eval( \$res );
    my $message = "Site \"$id\" ";
    $message .= ( $exists ? "exists!" : "does not exist!" );
    $site->set_results( "$message", \$res );
    Sling::Print::print_file_lock( $message, $log ) if ( defined );
    return $exists;
}
#}}}

#{{{sub list_members
sub list_members {
    my ( $site, $id ) = @_;
    my $res = Sling::Request::request( \$site,
        Sling::SiteUtil::list_members_setup( $site->{ 'BaseURL' }, $id ) );
    if ( Sling::SiteUtil::list_members_eval( \$res ) ) {
        my $content = $$res->content;
	# $content =~ /^.*"owners":\[([^\]]+)\].*/;
	# my $owners = $1;
        $site->set_results( "Site \"$id\" members are: $content.", $res );
	# $site->{ 'Members' } = "$members";
	return 1;
    }
    else {
        $site->set_results( "Unable to list members for site \"$id\"!", $res );
	# $site->{ 'Owners' } = "";
	return 0
    }
}
#}}}

#{{{sub remove_member
sub remove_member {
    my ( $site, $id, $member, $role ) = @_;
    my $res = Sling::Request::request( \$site,
        Sling::SiteUtil::remove_member_setup( $site->{ 'BaseURL' }, $id, $member, $role ) );
    if ( Sling::SiteUtil::remove_member_eval( $res ) ) {
        $site->set_results( "Site: \"$id\", member \"$member\" removed!", $res );
	return 1;
    }
    else {
        $site->set_results( "Site: \"$id\", member \"$member\" was not removed!", $res );
	return 0;
    }
}
#}}}

#{{{sub view
sub view {
    my ( $site, $id, $log ) = @_;
    my $res = Sling::Request::request( \$site,
        Sling::ContentUtil::exists_setup( $site->{ 'BaseURL' }, $id ) );
    my $success = Sling::ContentUtil::exists_eval( $res );
    my $message = ( $success ? $$res->content : "Problem viewing site: \"$id\"" );
    $site->set_results( "$message", $res );
    Sling::Print::print_file_lock( $message, $log ) if ( defined $log );
    return $success;
}
#}}}

1;
