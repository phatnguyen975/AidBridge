delete from users where email = 'letuanloc1412@gmail.com'


select * from volunteer_profiles

select current_location
from volunteer_profiles left join users u on volunteer_profiles.user_id = u.id
where u.email = 'letuanloc1412@gmail.com'

select * from users


