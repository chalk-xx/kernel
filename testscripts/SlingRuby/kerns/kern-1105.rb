#!/usr/bin/ruby

require 'net/http'

# connectivity information
SERVER = 'localhost'
PORT = 8080
PATH = '/dev/'
USER = 'admin'
PASS = 'admin'

# processing information
MAX_RUNS = 10

# make a call to the server to have the cookie set
res = Net::HTTP.get_response(SERVER, PATH, PORT)

if res.code == '200'
	cookie_header = res['Set-Cookie']
	eq_pos = cookie_header.index '='
	sc_pos = cookie_header.index ';'
	cookie_val = cookie_header[eq_pos + 1..sc_pos - 1]

	code = res.code
	count = 0
	while code == '200' and count < MAX_RUNS
		
		sleep(1)
		p count
		
		Net::HTTP.start(SERVER, PORT) do |http|
			headers = {'Cookie' => cookie_header}
			req = Net::HTTP::Head.new(PATH, headers)
			req.basic_auth(USER, PASS)
			res = http.request(req)

			if res.code == '200'
				req = Net::HTTP::Head.new('/var/cluster/user.cookie.json?c=' + cookie_val, headers)
				req.basic_auth(USER, PASS)
				res = http.request(req)
				code = res.code
				count += 1
			end
		end
	end
end
