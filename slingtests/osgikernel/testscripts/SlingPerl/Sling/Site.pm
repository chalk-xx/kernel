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
use JSON;
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
    my ( $class, $authn, $verbose, $log ) = @_;
    die "no authn provided!" unless defined $authn;
    my $response;
    my $site = { BaseURL => $$authn->{ 'BaseURL' },
                 Authn => $authn,
		 Message => "",
		 Owners => "",
		 Response => \$response,
		 Verbose => $verbose,
		 Log => $log };
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
    my ( $site, $id, $template, $joinable, $other_properties ) = @_;
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
    return $success;
}
#}}}

#{{{sub update_from_file
sub update_from_file {
    my ( $site, $file, $forkId, $numberForks ) = @_;
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
                $site->update( $id, $template, $joinable, \@properties );
		Sling::Print::print_result( $site );
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

#{{{sub delete
sub delete {
    my ( $site, $id ) = @_;
    my $res = Sling::Request::request( \$site,
        Sling::ContentUtil::delete_setup( $site->{ 'BaseURL' }, $id ) );
    my $success = Sling::ContentUtil::delete_eval( $res );
    my $message = "Site \"$id\" ";
    $message .= ( $success ? "deleted!" : "was not deleted!" );
    $site->set_results( "$message", $res );
    return $success;
}
#}}}

#{{{sub exists
sub exists {
    my ( $site, $id ) = @_;
    my $res = Sling::Request::request( \$site,
        Sling::ContentUtil::exists_setup( $site->{ 'BaseURL' }, $id ) );
    my $exists = Sling::ContentUtil::exists_eval( $res );
    my $message = "Site \"$id\" ";
    $message .= ( $exists ? "exists!" : "does not exist!" );
    $site->set_results( "$message", $res );
    return $exists;
}
#}}}

#{{{sub member_add
sub member_add {
    my ( $site, $id, $member ) = @_;
    my $res = Sling::Request::request( \$site,
        Sling::SiteUtil::member_add_setup( $site->{ 'BaseURL' }, $id, $member ) );
    my $success = Sling::SiteUtil::member_add_eval( $res );
    my $message = "Site \"$id\", member \"$member\" ";
    $message .= ( $success ? "added!" : "was not added!" );
    $site->set_results( "$message", $res );
    return $success;
}
#}}}

#{{{sub member_delete
sub member_delete{
    my ( $site, $id, $member ) = @_;
    my $res = Sling::Request::request( \$site,
        Sling::SiteUtil::member_delete_setup( $site->{ 'BaseURL' }, $id, $member ) );
    my $success = Sling::SiteUtil::member_delete_eval( $res );
    my $message = "Site \"$id\", member \"$member\" ";
    $message .= ( $success ? "deleted!" : "was not deleted!" );
    $site->set_results( "$message", $res );
    return $success;
}
#}}}

#{{{sub member_exists
sub member_exists {
    my ( $site, $actOnSite, $existsMember ) = @_;
    my $res = Sling::Request::request( \$site,
        Sling::SiteUtil::member_view_setup( $site->{ 'BaseURL' }, $actOnSite ) );
    my $success = Sling::SiteUtil::member_view_eval( $res );
    my $message;
    if ( $success ) {
        my $site_info = from_json( $$res->content );
	my $is_member = 0;
        foreach my $member ( @{ $site_info } ) {
            if ( $member->{ 'rep:userId' } =~ /^$existsMember$/ ) {
	        $is_member = 1;
		last;
	    }
        }
	$success = $is_member;
	$message = "\"$existsMember\" is " . ( $is_member ? "" : "not " ) .
	    "a member of site \"$actOnSite\"";
    }
    else {
        $message = "Problem viewing site: \"$actOnSite\"";
    }
    $site->set_results( "$message", $res );
    return $success;
}
#}}}

#{{{sub member_view
sub member_view {
    my ( $site, $actOnSite ) = @_;
    my $res = Sling::Request::request( \$site,
        Sling::SiteUtil::member_view_setup( $site->{ 'BaseURL' }, $actOnSite ) );
    my $success = Sling::SiteUtil::member_view_eval( $res );
    my $message;
    if ( $success ) {
        my $site_info = from_json( $$res->content );
        my $number_members = @{ $site_info };
        my $members = "Site \"$actOnSite\" has $number_members member(s):";
        foreach my $member ( @{ $site_info } ) {
            $members .= "\n" . $member->{ 'rep:userId' };
        }
	$message = "$members";
	$success = $number_members;
    }
    else {
        $message = "Problem viewing site: \"$actOnSite\"";
    }
    $site->set_results( "$message", $res );
    return $success;
}
#}}}

#{{{sub view
sub view {
    my ( $site, $id ) = @_;
    my $res = Sling::Request::request( \$site,
        Sling::ContentUtil::exists_setup( $site->{ 'BaseURL' }, $id ) );
    my $success = Sling::ContentUtil::exists_eval( $res );
    my $message = ( $success ? $$res->content : "Problem viewing site: \"$id\"" );
    $site->set_results( "$message", $res );
    return $success;
}
#}}}

1;
