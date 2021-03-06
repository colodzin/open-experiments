#!/usr/bin/env ruby

require 'sling/sling'
require 'sling/test'
require 'sling/contacts'
require 'test/unit.rb'
require 'test/unit/ui/console/testrunner.rb'
include SlingInterface
include SlingUsers
include SlingSites
include SlingContacts

class TC_MyContactTest < SlingTest

  def setup
    super
    @cm = ContactManager.new(@s)
  end

  def test_connect_users
    m = Time.now.to_i.to_s
    puts("Creating user aaron"+m)
    a = create_user("aaron"+m)
    puts("Creating user nico"+m)
    n = create_user("nico"+m)
    puts("Creating user ian"+m)
    i = create_user("ian"+m)
    @s.switch_user(a)
    puts("Aaron Adding Nico as a coworker and friend")
    res = @cm.invite_contact("nico"+m, [ "coworker", "friend" ])
    assert_equal("200", res.code, "Expected to be able to request contact addition")
    puts("Checking that The invitation to Nico is pending")
    contacts = @cm.get_pending()
    assert_not_nil(contacts, "Expected to get contacts back")
    assert_equal(contacts["results"].size, 1, "Expected single request back")
    contact = contacts["results"][0]
    assert_equal("nico"+m, contact["target"], "Expected nico to be my friend")
    assert_equal("PENDING", contact["details"]["sakai:state"], "Expected state to be 'PENDING'")
   

    @s.switch_user(n)
    puts("Operating as Nico")
    contacts = @cm.get_invited()
    assert_not_nil(contacts, "Expected to get an invite back ")
    assert_equal(contacts["results"].size, 1, "Only expecting a single invite ")
    contact = contacts["results"][0]
    assert_equal("aaron"+m,contact["target"], "Expected Aaron to be asking ")
    assert_equal("INVITED", contact["details"]["sakai:state"], "Expected state to be 'INVITED'") 
    res = @cm.accept_contact("aaron"+m)
    assert_equal("200", res.code, "Expecting acceptance of the contact")
    contacts = @cm.get_accepted()
    assert_not_nil(contacts, "Expected to get an accepted back ")
    assert_equal(contacts["results"].size, 1, "Only expecting a single acceptance ")
    contact = contacts["results"][0]
    assert_equal("aaron"+m,contact["target"], "Expected Nico to have been accepted ")
    assert_equal("ACCEPTED", contact["details"]["sakai:state"], "Expected state to be 'ACCEPTED'") 

    @s.switch_user(a)
    puts("Operating as Aaron")
    contacts = @cm.get_accepted()
    assert_not_nil(contacts, "Expected to get an accepted back ")
    assert_equal(contacts["results"].size, 1, "Only expecting a single acceptance ")
    contact = contacts["results"][0]
    assert_equal("nico"+m,contact["target"], "Expected Aaron to have been accepted ")
    assert_equal("ACCEPTED", contact["details"]["sakai:state"], "Expected state to be 'ACCEPTED'") 
 

  end

  def teardown
    @created_users.each do |user|
      @s.switch_user(user)
      contacts = @cm.get_all()
      contacts["results"].each do |result|
        assert_not_nil(result["target"], "Expected contacts to have names")
        res = @cm.remove_contact(result["target"])
        assert_equal("200", res.code, "Expected removal to succeed")
      end
    end
    super
  end

end

Test::Unit::UI::Console::TestRunner.run(TC_MyContactTest)

