#!/usr/bin/python

import urllib2
import simplejson
import sys, os

if len( sys.argv ) <= 3:
    print 'Usage: ' + os.path.basename( sys.argv[ 0 ] ) + " QUERY PAGE IP"
    sys.exit( 0 )

query = sys.argv[ 1 ].replace( ' ', '+' )
page  = sys.argv[ 2 ] # Not used, a place holder for consistency between engines
ip    = sys.argv[ 3 ]

# Only 64 results can be fetched using this method (8 pages of 8 results per page)
for page in range( 1, 9 ):
    start = str( ( page - 1 ) * 8 )

    search_url = ( 'https://ajax.googleapis.com/ajax/services/search/web'
        '?v=1.0&safe=active&rsz=large&start=' + start + '&q=' + query + 
        '&userip=' + ip )
    print search_url

    request = urllib2.Request( search_url, None, { 'Referer': ip } )
    response = urllib2.urlopen( request )

    results = simplejson.load( response )

    for result in results[ 'responseData' ][ 'results' ]:
        print result[ 'unescapedUrl' ]
