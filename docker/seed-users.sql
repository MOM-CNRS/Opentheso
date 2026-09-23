-- Seed accounts from docker/.env. Idempotent. Run with: make seed-users
--
-- Passwords are stored as unsalted MD5 hex, which is what the baseline dump uses and what
-- UserService.findByUsernameAndPasswordMigrated still accepts; the app rewrites the row as
-- BCrypt(12) on the first successful login. Re-running resets them to MD5, which is
-- harmless -- the next login migrates them again.

\set ON_ERROR_STOP on
BEGIN;

-- the project every non-superadmin account is scoped to
INSERT INTO user_group_label (label_group)
SELECT :'project'
WHERE NOT EXISTS (SELECT 1 FROM user_group_label WHERE label_group = :'project');

-- Rewrite the Liquibase-seeded admin in place, keyed on id_user = 1 rather than on the
-- username, so renaming it in .env leaves no account still reachable as admin/admin.
-- id_user = -1 is the anonymous-author sentinel for concept.creator defaults: never touch it.
UPDATE users SET
    username     = :'admin_user',
    password     = md5(:'admin_pass'),
    mail         = :'admin_user' || '@localhost',
    active       = true,
    issuperadmin = true,
    passtomodify = false
WHERE id_user = 1;

-- the three project-scoped accounts
INSERT INTO users (username, password, mail, active, issuperadmin, passtomodify)
SELECT v.uname, md5(v.pass), v.uname || '@localhost', true, false, false
FROM (VALUES (:'padmin_user'::text,  :'padmin_pass'::text),
             (:'manager_user',       :'manager_pass'),
             (:'contrib_user',       :'contrib_pass')) AS v(uname, pass)
ON CONFLICT (username) DO UPDATE
   SET password = EXCLUDED.password,
       mail     = EXCLUDED.mail,
       active   = true;

-- role links (2 = admin, 3 = manager, 4 = contributor).
-- The superadmin is linked as role 2 as well, purely so the project appears in its
-- "projects I administer" list -- issuperadmin already grants it everything.
INSERT INTO user_role_group (id_user, id_role, id_group)
SELECT u.id_user, v.role_id, g.id_group
FROM (VALUES (:'admin_user'::text, 2),
             (:'padmin_user',      2),
             (:'manager_user',     3),
             (:'contrib_user',     4)) AS v(uname, role_id)
JOIN users u            ON u.username = v.uname
JOIN user_group_label g ON g.label_group = :'project'
ON CONFLICT (id_user, id_group) DO UPDATE SET id_role = EXCLUDED.id_role;

COMMIT;

-- report what now exists
SELECT u.id_user, u.username, u.issuperadmin,
       coalesce(r.name, '-')        AS project_role,
       coalesce(g.label_group, '-') AS project
FROM users u
LEFT JOIN user_role_group urg ON urg.id_user  = u.id_user
LEFT JOIN roles r             ON r.id         = urg.id_role
LEFT JOIN user_group_label g  ON g.id_group   = urg.id_group
WHERE u.id_user > 0
ORDER BY u.id_user;
