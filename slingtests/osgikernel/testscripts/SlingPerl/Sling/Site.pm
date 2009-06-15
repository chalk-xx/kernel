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
    my ( $class, $url, $lwpUserAgent ) = @_;
    die "url not defined!" unless defined $url;
    die "no lwp user agent provided!" unless defined $lwpUserAgent;
    my $response;
    my $site = { BaseURL => "$url",
                 LWP => $lwpUserAgent,
		 Message => "",
		 Owners => "",
		 Response => \$response };
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

#{{{sub add
sub add {
    my ( $site, $id, $template, $joinable, $groups, $log ) = @_;
    my @properties = ("sling:resourceType=sakai/site");
    if ( defined $template ) {
        push ( @properties, "sakai:site-template=$template" );
    }
    if ( defined $joinable ) {
        push ( @properties, "sakai:joinable=$joinable" );
    }
    foreach my $group ( @{ $groups } ) {
        push ( @properties, "sakai:authorizables=$group" );
    }
    my $res = ${ $site->{ 'LWP' } }->request( Sling::Request::string_to_request(
        Sling::ContentUtil::add_setup( $site->{ 'BaseURL' }, $id, \@properties ), $site->{ 'LWP' } ) );
    my $success = Sling::ContentUtil::add_eval( \$res );
    my $message = "Site \"$id\" ";
    $message .= ( $success ? "added!" : "was not added!" );
    $site->set_results( "$message", \$res );
    Sling::Print::print_file_lock( $message, $log ) if ( defined $log );
    return $success;
}
#}}}

#{{{sub add_from_file
sub add_from_file {
    my ( $site, $file, $forkId, $numberForks, $log ) = @_;
    my $count = 0;
    open ( FILE, $file );
    while ( <FILE> ) {
        if ( $forkId == ( $count++ % $numberForks ) ) {
            chomp;
	    $_ =~ /^(.*?),(.*?),(.*?)$/;
	    my $id = $1;
	    my $name = $2;
	    my $description = $3;
	    if ( defined $id && defined $name && defined $description ) {
	        $site->add( $id, $name, $description, $log );
		Sling::Print::print_lock( $site->{ 'Message' } ) if ( ! defined $log );
	    }
	    else {
	        print "ERROR: Problem parsing site to add: \"$_\"\n";
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
    my $res = ${ $site->{ 'LWP' } }->request( Sling::Request::string_to_request(
        Sling::SiteUtil::add_member_setup( $site->{ 'BaseURL' }, $id, $member, $role ) ) );
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
    my $res = ${ $site->{ 'LWP' } }->request( Sling::Request::string_to_request(
                  Sling::ContentUtil::delete_setup( $site->{ 'BaseURL' }, $id ), $site->{ 'LWP' } ) );
    my $success = Sling::ContentUtil::delete_eval( \$res );
    my $message = "Site \"$id\" ";
    $message .= ( $success ? "deleted!" : "was not deleted!" );
    $site->set_results( "$message", \$res );
    Sling::Print::print_file_lock( $message, $log ) if ( defined $log );
    return $success;
}
#}}}

#{{{sub exists
sub exists {
    my ( $site, $id, $log ) = @_;
    my $res = ${ $site->{ 'LWP' } }->request( Sling::Request::string_to_request(
                  Sling::ContentUtil::exists_setup( $site->{ 'BaseURL' }, $id ), $site->{ 'LWP' } ) );
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
    my $res = ${ $site->{ 'LWP' } }->request( Sling::Request::string_to_request(
                  Sling::SiteUtil::list_members_setup( $site->{ 'BaseURL' }, $id ) ) );
    if ( Sling::SiteUtil::list_members_eval( \$res ) ) {
        my $content = $res->content;
	# $content =~ /^.*"owners":\[([^\]]+)\].*/;
	# my $owners = $1;
        $site->set_results( "Site \"$id\" members are: $content.", \$res );
	# $site->{ 'Members' } = "$members";
	return 1;
    }
    else {
        $site->set_results( "Unable to list members for site \"$id\"!", \$res );
	# $site->{ 'Owners' } = "";
	return 0
    }
}
#}}}

#{{{sub remove_member
sub remove_member {
    my ( $site, $id, $member, $role ) = @_;
    my $res = ${ $site->{ 'LWP' } }->request( Sling::Request::string_to_request(
        Sling::SiteUtil::remove_member_setup( $site->{ 'BaseURL' }, $id, $member, $role ) ) );
    if ( Sling::SiteUtil::remove_member_eval( \$res ) ) {
        $site->set_results( "Site: \"$id\", member \"$member\" removed!", \$res );
	return 1;
    }
    else {
        $site->set_results( "Site: \"$id\", member \"$member\" was not removed!", \$res );
	return 0;
    }
}
#}}}

#{{{sub view
sub view {
    my ( $site, $id, $log ) = @_;
    my $res = ${ $site->{ 'LWP' } }->request( Sling::Request::string_to_request(
                  Sling::ContentUtil::exists_setup( $site->{ 'BaseURL' }, $id ), $site->{ 'LWP' } ) );
    my $success = Sling::ContentUtil::exists_eval( \$res );
    my $message = ( $success ? $res->content : "Problem viewing site: \"$id\"" );
    $site->set_results( "$message", \$res );
    Sling::Print::print_file_lock( $message, $log ) if ( defined $log );
    return $success;
}
#}}}

1;
