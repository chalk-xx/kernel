#!/usr/bin/perl

package Sling::Group;

=head1 NAME

Group - group related functionality for Sakai implemented over rest
APIs.

=head1 ABSTRACT

Perl library providing a layer of abstraction to the REST group methods

=cut

#{{{imports
use strict;
use lib qw ( .. );
use Sling::GroupUtil;
use Sling::Util;
#}}}

#{{{sub new

=pod

=head2 new

Create, set up, and return a Group Object.

=cut

sub new {
    my ( $class, $url, $lwpUserAgent ) = @_;
    die "url not defined!" unless defined $url;
    die "no lwp user agent provided!" unless defined $lwpUserAgent;
    my $response;
    my $group = { BaseURL => "$url",
                 LWP => $lwpUserAgent,
		 Message => "",
		 Response => \$response };
    bless( $group, $class );
    return $group;
}
#}}}

#{{{sub set_results
sub set_results {
    my ( $group, $message, $response ) = @_;
    $group->{ 'Message' } = $message;
    $group->{ 'Response' } = $response;
    return 1;
}
#}}}

#{{{sub add
sub add {
    my ( $group, $actOnGroup, $log ) = @_;
    my $res = ${ $group->{ 'LWP' } }->request( Sling::Util::string_to_request(
        Sling::GroupUtil::add_setup( $group->{ 'BaseURL' }, $actOnGroup ) ) );
    my $success = Sling::GroupUtil::add_eval( \$res );
    my $message = "Group: \"$actOnGroup\" ";
    $message .= ( $success ? "added!" : "was not added!" );
    $group->set_results( "$message", \$res );
    print_file_lock( $message, $log ) if ( defined $log );
    return $success;
}
#}}}

#{{{sub delete
sub delete {
    my ( $group, $actOnGroup, $log ) = @_;
    my $res = ${ $group->{ 'LWP' } }->request( Sling::Util::string_to_request(
        Sling::GroupUtil::delete_setup( $group->{ 'BaseURL' }, $actOnGroup ) ) );
    my $success = Sling::GroupUtil::delete_eval( \$res );
    my $message = "Group: \"$actOnGroup\" ";
    $message .= ( $success ? "deleted!" : "was not deleted!" );
    $group->set_results( "$message", \$res );
    print_file_lock( $message, $log ) if ( defined $log );
    return $success;
}
#}}}

#{{{sub add_from_file
sub add_from_file {
    my ( $group, $file, $forkId, $numberForks, $log ) = @_;
    my $count = 0;
    open ( FILE, $file );
    while ( <FILE> ) {
        if ( $forkId == ( $count++ % $numberForks ) ) {
            chomp;
	    $_ =~ /^(.*?)$/;
	    my $actOnGroup = $1;
	    if ( defined $actOnGroup ) {
	        $group->add( $actOnGroup, $log );
		Sling::Util::print_lock( $group->{ 'Message' } );
	    }
	}
    }
    close ( FILE ); 
    return 1;
}
#}}}

#{{{sub exists
sub exists {
    my ( $group, $actOnGroup, $log ) = @_;
    my $res = ${ $group->{ 'LWP' } }->request( Sling::Util::string_to_request(
                  Sling::GroupUtil::exists_setup( $group->{ 'BaseURL' }, $actOnGroup ) ) );
    my $success = Sling::GroupUtil::exists_eval( \$res );
    my $message = "Group \"$actOnGroup\" ";
    $message .= ( $success ? "exists!" : "does not exist!" );
    $group->set_results( "$message", \$res );
    print_file_lock( $message, $log ) if ( defined $log );
    return $success;
}
#}}}

#{{{sub view
sub view {
    my ( $group, $actOnGroup, $log ) = @_;
    my $res = ${ $group->{ 'LWP' } }->request( Sling::Util::string_to_request(
                  Sling::GroupUtil::view_setup( $group->{ 'BaseURL' }, $actOnGroup ) ) );
    my $success = Sling::GroupUtil::view_eval( \$res );
    my $message = ( $success ? $res->content : "Problem viewing group: \"$actOnGroup\"" );
    $group->set_results( "$message", \$res );
    print_file_lock( $message, $log ) if ( defined $log );
    return $success;
}
#}}}

1;
