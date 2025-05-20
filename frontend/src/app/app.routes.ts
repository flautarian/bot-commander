import { Routes } from '@angular/router';


export const routes: Routes = [
    {
        path: 'frontpage',
        loadChildren: () => import('./frontpage/frontpage.module').then(m => m.FrontpageModule)
    },
    { path: '', redirectTo: '/frontpage', pathMatch: 'full' },
    { path: '**', redirectTo: '/frontpage' }
];